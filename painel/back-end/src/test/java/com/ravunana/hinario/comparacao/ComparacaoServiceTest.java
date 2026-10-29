package com.ravunana.hinario.comparacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.services.DataFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PRD 5.7.3 e o exemplo do 3.2: qualquer diferença é listada. */
class ComparacaoServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private ComparacaoService comparacao;

    @BeforeEach
    void preparar() {
        comparacao = new ComparacaoService(new DataFiles(temp.toString()));
    }

    private JsonNode gemini(String... linhas) {
        ObjectNode raiz = MAPPER.createObjectNode();
        ObjectNode bloco = raiz.putArray("blocos").addObject();
        bloco.put("tipo", "estrofe");
        ArrayNode array = bloco.putArray("linhas");
        for (String linha : linhas) {
            array.addObject().put("texto", linha).put("recuo", 0);
        }
        return raiz;
    }

    private JsonNode tesseract(String... linhas) {
        ObjectNode raiz = MAPPER.createObjectNode();
        ArrayNode array = raiz.putArray("linhas");
        for (String linha : linhas) {
            array.addObject().put("texto", linha).put("confianca", 0.91);
        }
        return raiz;
    }

    @Test
    void leiturasIguaisNaoGeramDivergencias() {
        ArrayNode saida = comparacao.comparar(
                gemini("M’ uhukunii aJesus", "[?] kuanza"),
                tesseract("M’ uhukunii aJesus", "[?] kuanza"));
        assertTrue(saida.isEmpty());
    }

    @Test
    void diferencaDeAcentoÉAssinalada() {
        ArrayNode saida = comparacao.comparar(
                gemini("Sai-ku akua diniota"),
                tesseract("Sai-ku akua díníota"));
        assertEquals(1, saida.size());
        JsonNode divergencia = saida.get(0);
        assertEquals("so_acento", divergencia.get("tipo").asText());
        assertEquals("Sai-ku akua diniota", divergencia.get("gemini").asText());
        assertEquals("Sai-ku akua díníota", divergencia.get("tesseract").asText());
        assertEquals(0, divergencia.get("bloco").asInt());
        assertEquals(0, divergencia.get("linha").asInt());
    }

    @Test
    void exemploDoPrdDePontuacao() {
        // PRD 3.2: "Sai-ku akua diniota" vs "Sai-ku akua diniota."
        ArrayNode saida = comparacao.comparar(
                gemini("Sai-ku akua diniota"),
                tesseract("Sai-ku akua diniota."));
        assertEquals(1, saida.size());
        assertEquals("so_pontuacao", saida.get(0).get("tipo").asText());
    }

    @Test
    void palavraDiferenteÉDivergente() {
        ArrayNode saida = comparacao.comparar(
                gemini("kuanza kwetu"),
                tesseract("kuanza kwenu"));
        assertEquals(1, saida.size());
        assertEquals("divergente", saida.get(0).get("tipo").asText());
    }

    @Test
    void linhaSoDoTesseractVemSemPosicaoDoGemini() {
        ArrayNode saida = comparacao.comparar(
                gemini("linha uma"),
                tesseract("linha uma", "linha fantasma"));
        assertEquals(1, saida.size());
        JsonNode divergencia = saida.get(0);
        assertTrue(divergencia.get("bloco").isNull());
        assertTrue(divergencia.get("gemini").isNull());
        assertEquals("linha fantasma", divergencia.get("tesseract").asText());
        assertEquals("divergente", divergencia.get("tipo").asText());
    }

    @Test
    void linhaSoDoGeminiVemSemTextoDoTesseract() {
        ArrayNode saida = comparacao.comparar(
                gemini("linha uma", "linha dois"),
                tesseract("linha uma"));
        assertEquals(1, saida.size());
        JsonNode divergencia = saida.get(0);
        assertEquals(0, divergencia.get("bloco").asInt());
        assertEquals(1, divergencia.get("linha").asInt());
        assertEquals("linha dois", divergencia.get("gemini").asText());
        assertTrue(divergencia.get("tesseract").isNull());
    }

    @Test
    void apontaParaOBlocoERealDaResposta() {
        // bloco 0 = título (sem linhas), bloco 1 = estrofe: a divergência é da 1.ª linha da estrofe
        ObjectNode raiz = MAPPER.createObjectNode();
        ArrayNode blocos = raiz.putArray("blocos");
        blocos.addObject().put("tipo", "titulo_hino").putArray("linhas");
        ObjectNode estrofe = blocos.addObject();
        ArrayNode linhas = estrofe.putArray("linhas");
        linhas.addObject().put("texto", "M’ uhukunii aJesus").put("recuo", 0);

        ArrayNode saida = comparacao.comparar(raiz, tesseract()); // sem linhas do Tesseract
        assertEquals(1, saida.size());
        assertEquals(1, saida.get(0).get("bloco").asInt());
        assertEquals(0, saida.get(0).get("linha").asInt());
    }

    @Test
    void respostasMalformadasNaoExplodem() {
        assertTrue(comparacao.comparar(MAPPER.createObjectNode(), MAPPER.createObjectNode()).isEmpty());
        assertTrue(comparacao.comparar(null, tesseract("x")).size() >= 0);
    }
}
