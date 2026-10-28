package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Revalidação em Java da resposta do Gemini (PRD 5.4 + 5.6): o esquema é
 * enviado na API **e** verificado aqui, e respostas com Markdown ou numeração
 * dentro do texto são rejeitadas para repetir.
 *
 * Não altera texto nenhum: só valida e rejeita.
 */
@Component
public class RespostaGeminiValidador {

    private static final Set<String> TIPOS_BLOCO = Set.of("titulo_hino", "estrofe", "bloco_sem_numero");

    /** Sintomas de Markdown: negrito, títulos, crases/triplos-cravetas. */
    private static final Pattern MARKDOWN = Pattern.compile("(\\*\\*|`{1,3}|^#)");

    /** Número de estrofe embutido no início da linha (o número tem de estar no campo). */
    private static final Pattern NUMERACAO_INICIO = Pattern.compile("^\\s*\\d{1,3}[\\s.)\\-–—]");

    /**
     * @throws GeminiApiExcecao tipo {@code RESPOSTA_INVALIDA} com o motivo
     */
    public void validar(JsonNode raiz) throws GeminiApiExcecao {
        if (raiz == null || !raiz.isObject()) {
            throw invalida("a resposta não é um objeto JSON");
        }
        JsonNode blocos = raiz.get("blocos");
        if (blocos == null || !blocos.isArray()) {
            throw invalida("falta o array \"blocos\"");
        }
        verificarTexto(raiz.get("cabecalhoPagina"), "cabecalhoPagina");
        verificarNumero(raiz.get("numeroPaginaImpresso"), "numeroPaginaImpresso");

        for (int i = 0; i < blocos.size(); i++) {
            JsonNode bloco = blocos.get(i);
            String onde = "blocos[" + i + "]";
            if (!bloco.isObject()) {
                throw invalida(onde + " não é um objeto");
            }
            JsonNode tipo = bloco.get("tipo");
            if (tipo == null || !tipo.isTextual() || !TIPOS_BLOCO.contains(tipo.asText())) {
                throw invalida(onde + ".tipo inválido: " + (tipo == null ? "em falta" : tipo.toString()));
            }
            verificarNumero(bloco.get("numeroHino"), onde + ".numeroHino");
            verificarNumero(bloco.get("numeroEstrofe"), onde + ".numeroEstrofe");
            verificarTexto(bloco.get("titulo"), onde + ".titulo");

            JsonNode linhas = bloco.get("linhas");
            if (linhas == null || !linhas.isArray()) {
                throw invalida(onde + ".linhas em falta ou não é array");
            }
            if ("titulo_hino".equals(tipo.asText()) && !linhas.isEmpty()) {
                throw invalida(onde + ": titulo_hino não pode ter linhas");
            }
            for (int j = 0; j < linhas.size(); j++) {
                JsonNode linha = linhas.get(j);
                String aqui = onde + ".linhas[" + j + "]";
                if (!linha.isObject()) {
                    throw invalida(aqui + " não é um objeto");
                }
                JsonNode texto = linha.get("texto");
                if (texto == null || !texto.isTextual()) {
                    throw invalida(aqui + ".texto em falta ou não é texto");
                }
                verificarTexto(texto, aqui + ".texto");
                JsonNode recuo = linha.get("recuo");
                if (recuo != null && !recuo.isNull()
                        && (!recuo.isInt() || recuo.asInt() < 0 || recuo.asInt() > 2)) {
                    throw invalida(aqui + ".recuo tem de ser 0, 1 ou 2");
                }
            }
            verificarDuvidas(bloco.get("duvidas"), onde + ".duvidas");
        }
    }

    private void verificarTexto(JsonNode valor, String onde) throws GeminiApiExcecao {
        if (valor == null || valor.isNull()) {
            return;
        }
        if (!valor.isTextual()) {
            throw invalida(onde + " não é texto");
        }
        String texto = valor.asText();
        if (MARKDOWN.matcher(texto).find()) {
            throw invalida(onde + " contém Markdown: " + texto);
        }
        if (NUMERACAO_INICIO.matcher(texto).find()) {
            throw invalida(onde + " contém numeração no texto: " + texto);
        }
    }

    private void verificarNumero(JsonNode valor, String onde) throws GeminiApiExcecao {
        if (valor == null || valor.isNull()) {
            return;
        }
        if (!valor.isInt()) {
            throw invalida(onde + " tem de ser número inteiro ou null: " + valor);
        }
    }

    private void verificarDuvidas(JsonNode duvidas, String onde) throws GeminiApiExcecao {
        if (duvidas == null || duvidas.isNull()) {
            return;
        }
        if (!duvidas.isArray()) {
            throw invalida(onde + " não é array");
        }
        for (int i = 0; i < duvidas.size(); i++) {
            JsonNode duvida = duvidas.get(i);
            if (!duvida.isObject() || duvida.get("trecho") == null || !duvida.get("trecho").isTextual()) {
                throw invalida(onde + "[" + i + "] inválida (esperado objeto com \"trecho\")");
            }
        }
    }

    private static GeminiApiExcecao invalida(String motivo) {
        return new GeminiApiExcecao(GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA, 200, motivo);
    }
}
