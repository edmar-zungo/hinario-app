package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Respostas simuladas da API do Gemini para os testes (PRD 5.2 e 5.4). */
final class RespostasGemini {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RespostasGemini() {
    }

    /** Corpo do esquema 5.4 como o modelo o devolve dentro do candidato. */
    static String esquema(int pagina) {
        ObjectNode raiz = MAPPER.createObjectNode();
        raiz.put("numeroPaginaImpresso", pagina);
        raiz.putNull("cabecalhoPagina");
        ArrayNode blocos = raiz.putArray("blocos");

        ObjectNode titulo = blocos.addObject();
        titulo.put("tipo", "titulo_hino");
        titulo.put("numeroHino", 12);
        titulo.put("titulo", "LUZ DO MUNDO");
        titulo.putNull("numeroEstrofe");
        titulo.put("italico", false);
        titulo.put("continuacaoDeBlocoAnterior", false);
        titulo.putArray("linhas");
        titulo.putArray("duvidas");

        ObjectNode estrofe = blocos.addObject();
        estrofe.put("tipo", "estrofe");
        estrofe.putNull("numeroHino");
        estrofe.putNull("titulo");
        estrofe.put("numeroEstrofe", 1);
        estrofe.put("italico", false);
        estrofe.put("continuacaoDeBlocoAnterior", false);
        ArrayNode linhasEstrofe = estrofe.putArray("linhas");
        linhasEstrofe.addObject().put("texto", "M’ uhukunii aJesus").put("recuo", 0);
        linhasEstrofe.addObject().put("texto", "[?] kuanza").put("recuo", 1);
        estrofe.putArray("duvidas");
        return raiz.toString();
    }

    /** Envelope 200 da API com o texto do candidato. */
    static GeminiHttp.Resposta ok(String textoDoCandidato) {
        ObjectNode raiz = MAPPER.createObjectNode();
        ArrayNode candidatos = raiz.putArray("candidates");
        ObjectNode cand = candidatos.addObject();
        ObjectNode content = cand.putObject("content");
        content.putArray("parts").addObject().put("text", textoDoCandidato);
        cand.put("finishReason", "STOP");
        return new GeminiHttp.Resposta(200, raiz.toString());
    }

    /** Envelope 200 com resposta válida do esquema 5.4. */
    static GeminiHttp.Resposta okValida(int pagina) {
        return ok(esquema(pagina));
    }

    /** 200 mas o candidato veio bloqueado pela segurança. */
    static GeminiHttp.Resposta bloqueada() {
        ObjectNode raiz = MAPPER.createObjectNode();
        ArrayNode candidatos = raiz.putArray("candidates");
        candidatos.addObject().put("finishReason", "SAFETY");
        return new GeminiHttp.Resposta(200, raiz.toString());
    }

    /** Bloqueio reportado no promptFeedback. */
    static GeminiHttp.Resposta bloqueadaPorFeedback() {
        ObjectNode raiz = MAPPER.createObjectNode();
        raiz.putObject("promptFeedback").put("blockReason", "SAFETY");
        raiz.putArray("candidates");
        return new GeminiHttp.Resposta(200, raiz.toString());
    }

    static GeminiHttp.Resposta status(int status, String corpo) {
        return new GeminiHttp.Resposta(status, corpo);
    }
}
