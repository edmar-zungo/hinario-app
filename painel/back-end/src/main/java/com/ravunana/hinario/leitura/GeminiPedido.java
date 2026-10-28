package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * Protocolo da API do Gemini (PRD 5.2): monta o pedido com a instrução 5.3,
 * o esquema 5.4 e o contexto 5.5; classifica as falhas HTTP; extrai e
 * valida a resposta (5.6).
 *
 * Não faz política de rotação — isso é o {@link ModelPool}.
 */
@Component
public class GeminiPedido {

    private static final Set<String> MOTIVOS_BLOQUEIO = Set.of(
            "SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "IMAGE_SAFETY");
    private static final Set<String> MOTIVOS_INVALIDOS = Set.of("MAX_TOKENS", "IMAGE_SAFETY");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String INSTRUCAO = carregarInstrucao();

    private final GeminiConfig config;
    private final GeminiHttp http;
    private final RespostaGeminiValidador validador;

    public GeminiPedido(GeminiConfig config, GeminiHttp http, RespostaGeminiValidador validador) {
        this.config = config;
        this.http = http;
        this.validador = validador;
    }

    /**
     * Chamada completa a um modelo: pedido, resposta e validação.
     *
     * @return a resposta do esquema 5.4, validada
     * @throws GeminiApiExcecao com a classificação da falha (5.6)
     */
    public JsonNode pedir(String modelo, byte[] jpeg, String contexto) throws GeminiApiExcecao {
        GeminiHttp.Resposta resposta = http.gerarConteudo(modelo, corpo(modelo, jpeg, contexto));
        if (resposta.status() != 200) {
            throw classificar(resposta.status(), resposta.corpo());
        }
        return extrair(resposta.corpo());
    }

    /** Corpo JSON do pedido (PRD 5.2). */
    public String corpo(String modelo, byte[] jpeg, String contexto) {
        ObjectNode pedido = MAPPER.createObjectNode();

        ObjectNode instrucao = MAPPER.createObjectNode();
        instrucao.putArray("parts").addObject().put("text", INSTRUCAO);
        pedido.set("systemInstruction", instrucao);

        ObjectNode conteudo = MAPPER.createObjectNode();
        conteudo.put("role", "user");
        ArrayNode partes = conteudo.putArray("parts");
        ObjectNode imagem = partes.addObject();
        ObjectNode inline = imagem.putObject("inlineData");
        inline.put("mimeType", "image/jpeg");
        inline.put("data", Base64.getEncoder().encodeToString(jpeg));
        partes.addObject().put("text", contexto);
        pedido.putArray("contents").add(conteudo);

        ObjectNode geracao = pedido.putObject("generationConfig");
        geracao.put("responseMimeType", "application/json");
        geracao.set("responseSchema", esquemaResposta());
        geracao.put("candidateCount", 1);
        geracao.put("temperature", config.temperaturaDe(modelo));
        String thinking = config.getThinkingLevel();
        if (thinking != null && !thinking.isBlank()) {
            geracao.putObject("thinkingConfig").put("thinkingLevel", thinking);
        }
        return pedido.toString();
    }

    /**
     * Classifica uma resposta não-200 (PRD 5.6 e 5.8).
     * 429 por dia é distinguido do 429 por minuto pelo texto do erro.
     */
    public static GeminiApiExcecao classificar(int status, String corpo) {
        String corpoBaixo = corpo == null ? "" : corpo.toLowerCase(Locale.ROOT);
        if (status == 429) {
            boolean diario = corpoBaixo.contains("per day") || corpoBaixo.contains("daily")
                    || corpoBaixo.contains("por dia") || corpoBaixo.contains("24 hour")
                    || corpoBaixo.contains("24-hour");
            return new GeminiApiExcecao(
                    diario ? GeminiApiExcecao.Tipo.QUOTA_DIARIA : GeminiApiExcecao.Tipo.QUOTA_MINUTO,
                    status, resumo(corpo));
        }
        if (status >= 500) {
            return new GeminiApiExcecao(GeminiApiExcecao.Tipo.TEMPORARIO, status, resumo(corpo));
        }
        if (status == 403) {
            return new GeminiApiExcecao(GeminiApiExcecao.Tipo.ERRO, status,
                    "chave recusada (403) — verificar GEMINI_API_KEY e o projeto sem faturação");
        }
        return new GeminiApiExcecao(GeminiApiExcecao.Tipo.ERRO, status, resumo(corpo));
    }

    /** Extrai o JSON do candidato, tratando bloqueios e respostas vazias (5.6). */
    public JsonNode extrair(String corpoHttp) throws GeminiApiExcecao {
        JsonNode raiz;
        try {
            raiz = MAPPER.readTree(corpoHttp == null ? "" : corpoHttp);
        } catch (IOException e) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200,
                    "corpo de resposta não é JSON");
        }
        if (raiz == null || !raiz.isObject()) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200,
                    "corpo de resposta não é um objeto");
        }

        JsonNode feedback = raiz.path("promptFeedback");
        if (feedback.path("blockReason").isTextual()) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.BLOQUEADO, 200,
                    "bloqueado: " + feedback.path("blockReason").asText());
        }
        JsonNode candidatos = raiz.path("candidates");
        if (!candidatos.isArray() || candidatos.isEmpty()) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200,
                    "sem candidatos na resposta");
        }
        JsonNode candidato = candidatos.get(0);
        String finish = candidato.path("finishReason").asText("");
        if (MOTIVOS_BLOQUEIO.contains(finish)) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.BLOQUEADO, 200, "bloqueado: " + finish);
        }
        if (MOTIVOS_INVALIDOS.contains(finish)) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200,
                    "resposta incompleta: " + finish);
        }

        StringBuilder texto = new StringBuilder();
        JsonNode partes = candidato.path("content").path("parts");
        if (partes.isArray()) {
            for (JsonNode parte : partes) {
                if (parte.path("text").isTextual()) {
                    texto.append(parte.path("text").asText());
                }
            }
        }
        if (texto.isEmpty()) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200, "resposta vazia");
        }

        JsonNode esquema;
        try {
            esquema = MAPPER.readTree(texto.toString());
        } catch (IOException e) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200,
                    "o modelo não devolveu JSON válido");
        }
        validador.validar(esquema);
        return esquema;
    }

    /** Esquema 5.4 no formato da API (`responseSchema`). */
    public static JsonNode esquemaResposta() {
        ObjectNode esquema = MAPPER.createObjectNode();
        esquema.put("type", "OBJECT");
        ObjectNode propriedades = esquema.putObject("properties");

        propriedades.putObject("numeroPaginaImpresso")
                .put("type", "INTEGER").put("nullable", true);
        propriedades.putObject("cabecalhoPagina")
                .put("type", "STRING").put("nullable", true);

        ObjectNode blocos = propriedades.putObject("blocos");
        blocos.put("type", "ARRAY");
        ObjectNode itemBloco = blocos.putObject("items");
        itemBloco.put("type", "OBJECT");
        ObjectNode propsBloco = itemBloco.putObject("properties");
        itemBloco.putArray("required").add("tipo").add("linhas");

        propsBloco.putObject("tipo").put("type", "STRING")
                .putArray("enum").add("titulo_hino").add("estrofe").add("bloco_sem_numero");
        propsBloco.putObject("numeroHino").put("type", "INTEGER").put("nullable", true);
        propsBloco.putObject("titulo").put("type", "STRING").put("nullable", true);
        propsBloco.putObject("numeroEstrofe").put("type", "INTEGER").put("nullable", true);
        propsBloco.putObject("italico").put("type", "BOOLEAN").put("nullable", true);
        propsBloco.putObject("continuacaoDeBlocoAnterior").put("type", "BOOLEAN").put("nullable", true);

        ObjectNode linhas = propsBloco.putObject("linhas");
        linhas.put("type", "ARRAY");
        ObjectNode itemLinha = linhas.putObject("items");
        itemLinha.put("type", "OBJECT");
        ObjectNode propsLinha = itemLinha.putObject("properties");
        propsLinha.putObject("texto").put("type", "STRING");
        propsLinha.putObject("recuo").put("type", "INTEGER");
        itemLinha.putArray("required").add("texto");

        ObjectNode duvidas = propsBloco.putObject("duvidas");
        duvidas.put("type", "ARRAY");
        ObjectNode itemDuvida = duvidas.putObject("items");
        itemDuvida.put("type", "OBJECT");
        ObjectNode propsDuvida = itemDuvida.putObject("properties");
        propsDuvida.putObject("linha").put("type", "INTEGER");
        propsDuvida.putObject("trecho").put("type", "STRING");
        propsDuvida.putObject("motivo").put("type", "STRING");
        itemDuvida.putArray("required").add("linha").add("trecho");

        esquema.putArray("required").add("blocos");
        return esquema;
    }

    private static String resumo(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return "sem corpo";
        }
        String limpo = corpo.replaceAll("\\s+", " ").trim();
        return limpo.length() > 300 ? limpo.substring(0, 300) + "…" : limpo;
    }

    private static String carregarInstrucao() {
        try (InputStream entrada = GeminiPedido.class.getResourceAsStream("/leitura/instrucao-sistema.txt")) {
            if (entrada == null) {
                throw new IllegalStateException("falta /leitura/instrucao-sistema.txt (PRD 5.3)");
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("não foi possível ler a instrução do sistema (PRD 5.3)", e);
        }
    }
}
