package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.comparacao.AlfabetoVerificador;
import com.ravunana.hinario.estrutura.EstruturaService;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Leitura síncrona de páginas com Gemini (sem fila em segundo plano):
 * valida a página, executa o OCR, guarda o resultado e estrutura os blocos.
 */
@Service
public class LeituraService {

    /** Estados a partir dos quais se pode (re)ler uma página. */
    private static final Set<EstadoPagina> LECIVEIS = EnumSet.of(
            EstadoPagina.IMPORTADA, EstadoPagina.EM_FILA, EstadoPagina.PAUSADA_QUOTA,
            EstadoPagina.LIDA, EstadoPagina.ERRO_LEITURA, EstadoPagina.BLOQUEADA_PELA_IA);

    private final GeminiProvider gemini;
    private final GeminiConfig configGemini;
    private final AlfabetoVerificador alfabeto;
    private final EstruturaService estruturas;
    private final PaginaService paginas;
    private final DataFiles dataFiles;

    public LeituraService(GeminiProvider gemini, GeminiConfig configGemini,
                          AlfabetoVerificador alfabeto, EstruturaService estruturas,
                          PaginaService paginas, DataFiles dataFiles) {
        this.gemini = gemini;
        this.configGemini = configGemini;
        this.alfabeto = alfabeto;
        this.estruturas = estruturas;
        this.paginas = paginas;
        this.dataFiles = dataFiles;
    }

    /** Executa a leitura e a estruturação da página de forma síncrona. */
    public Pagina ler(int numero) {
        Pagina pagina;
        try {
            pagina = paginas.obter(numero);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível ler a página " + numero + ": " + e.getMessage());
        }
        if (!gemini.disponivel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, configGemini.isEnabled()
                    ? "GEMINI_API_KEY não definida — leitura indisponível"
                    : "Leitura Gemini desactivada");
        }
        if (!LECIVEIS.contains(pagina.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "estado " + pagina.estado().getValor() + " não permite nova leitura");
        }

        try {
            LeituraResultado resultado = gemini.ler(pagina);
            return gravar(pagina, resultado, null, EstadoPagina.LIDA);
        } catch (QuotaEsgotadaExcecao e) {
            return gravar(pagina, null, e, EstadoPagina.PAUSADA_QUOTA);
        } catch (BloqueadaExcecao e) {
            return gravar(pagina, null, e, EstadoPagina.BLOQUEADA_PELA_IA);
        } catch (LeituraExcecao e) {
            return gravar(pagina, null, e, EstadoPagina.ERRO_LEITURA);
        }
    }

    private Pagina gravar(Pagina pagina, LeituraResultado leituraGemini,
                          LeituraExcecao falhaGemini, EstadoPagina estado) {
        ObjectNode leituras = dataFiles.mapper().createObjectNode();
        if (pagina.leituras() != null && pagina.leituras().isObject()) {
            pagina.leituras().fields().forEachRemaining(c -> {
                if (!"tesseract".equals(c.getKey())) {
                    leituras.set(c.getKey(), c.getValue());
                }
            });
        }
        if (leituraGemini != null || falhaGemini != null) {
            leituras.set("gemini", geminiJson(leituraGemini, falhaGemini));
        }

        if (leituraGemini != null) {
            ObjectNode geminiNo = (ObjectNode) leituras.get("gemini");
            ObjectNode validacao = geminiNo.putObject("validacao");
            validacao.set("alfabeto", alfabeto.verificarResposta(leituraGemini.resposta()));
        }

        try {
            Pagina atualizada = pagina.comLeituras(leituras).comEstado(estado);
            Integer numeroImpresso = numeroPaginaImpresso(leituraGemini);
            if (numeroImpresso != null && numeroImpresso > 0 && numeroImpresso != atualizada.pagina()) {
                atualizada = paginas.renomear(atualizada, numeroImpresso);
            } else {
                paginas.atualizar(atualizada);
            }
            if (estado == EstadoPagina.LIDA) {
                try {
                    return estruturas.estruturar(atualizada.pagina());
                } catch (Exception ignorada) {
                    // a leitura Gemini fica guardada mesmo se a estruturação falhar
                }
            }
            return atualizada;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível gravar a leitura da página " + pagina.pagina() + ": " + e.getMessage());
        }
    }

    private static Integer numeroPaginaImpresso(LeituraResultado leituraGemini) {
        if (leituraGemini == null || leituraGemini.resposta() == null) {
            return null;
        }
        JsonNode numero = leituraGemini.resposta().get("numeroPaginaImpresso");
        return (numero != null && numero.isInt()) ? numero.asInt() : null;
    }

    private ObjectNode geminiJson(LeituraResultado resultado, LeituraExcecao falha) {
        ObjectNode gemini = dataFiles.mapper().createObjectNode();
        List<LeituraResultado.Tentativa> tentativas;
        if (resultado != null) {
            gemini.put("modelo", resultado.modelo());
            tentativas = resultado.tentativas();
            gemini.set("resposta", resultado.resposta());
        } else {
            tentativas = falha == null ? List.of() : falha.getTentativas();
            String ultimoModelo = tentativas.isEmpty() ? null : tentativas.get(tentativas.size() - 1).modelo();
            if (ultimoModelo != null) {
                gemini.put("modelo", ultimoModelo);
            }
            if (falha != null) {
                gemini.put("erro", falha.getMessage() == null ? "falha de leitura" : falha.getMessage());
            }
        }
        ArrayNode array = gemini.putArray("tentativas");
        for (LeituraResultado.Tentativa t : tentativas) {
            ObjectNode registo = array.addObject();
            registo.put("modelo", t.modelo());
            registo.put("resultado", t.resultado());
            registo.put("quando", t.quando());
            registo.put("origem", t.origem());
        }
        return gemini;
    }
}

