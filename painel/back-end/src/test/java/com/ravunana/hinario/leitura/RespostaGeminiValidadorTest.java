package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rejeição de respostas inválidas (PRD 5.6): JSON fora do esquema, Markdown,
 * numeração dentro do texto, recuo fora do intervalo.
 */
class RespostaGeminiValidadorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RespostaGeminiValidador validador = new RespostaGeminiValidador();

    private void validar(String json) throws Exception {
        validador.validar(MAPPER.readTree(json));
    }

    private static GeminiApiExcecao rejeita(String json) throws Exception {
        RespostaGeminiValidador validador = new RespostaGeminiValidador();
        return assertThrows(GeminiApiExcecao.class, () -> validador.validar(MAPPER.readTree(json)));
    }

    @Test
    void aceitaRespostaValidaDoEsquema() {
        assertDoesNotThrow(() -> validar(RespostasGemini.esquema(395)));
    }

    @Test
    void aceitaBlocoVazio() {
        assertDoesNotThrow(() -> validar("{\"blocos\":[]}"));
    }

    @Test
    void rejeitaJsonNaoObjeto() throws Exception {
        assertTrue(rejeita("[1,2]").getTipo() == GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA);
        assertTrue(rejeita("\"texto\"").getTipo() == GeminiApiExcecao.Tipo.RESPOSTA_INVALIDA);
    }

    @Test
    void rejeitaSemBlocos() throws Exception {
        GeminiApiExcecao e = rejeita("{\"outraCoisa\": 1}");
        assertTrue(e.getMessage().contains("blocos"));
    }

    @Test
    void rejeitaTipoDeBlocoDesconhecido() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"coro\",\"linhas\":[]}]}");
        assertTrue(e.getMessage().contains("tipo"));
    }

    @Test
    void rejeitaRecuoForaDoIntervalo() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"estrofe\",\"linhas\":["
                + "{\"texto\":\"abc\",\"recuo\":3}]}]}");
        assertTrue(e.getMessage().contains("recuo"));
    }

    @Test
    void rejeitaLinhaSemTexto() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"estrofe\",\"linhas\":[{\"recuo\":0}]}]}");
        assertTrue(e.getMessage().contains("texto"));
    }

    @Test
    void rejeitaMarkdown() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"estrofe\",\"linhas\":["
                + "{\"texto\":\"**M’ uhukunii**\",\"recuo\":0}]}]}");
        assertTrue(e.getMessage().contains("Markdown"));
    }

    @Test
    void rejeitaTituloComCrase() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"titulo_hino\",\"titulo\":\"`Luz do Mundo`\","
                + "\"linhas\":[]}]}");
        assertTrue(e.getMessage().contains("Markdown"));
    }

    @Test
    void rejeitaNumeracaoDentroDoTexto() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"estrofe\",\"linhas\":["
                + "{\"texto\":\"1 M’ uhukunii\",\"recuo\":0}]}]}");
        assertTrue(e.getMessage().contains("numeração"));
    }

    @Test
    void rejeitaTituloHinoComLinhas() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"titulo_hino\","
                + "\"linhas\":[{\"texto\":\"LUZ DO MUNDO\",\"recuo\":0}]}]}");
        assertTrue(e.getMessage().contains("titulo_hino"));
    }

    @Test
    void rejeitaNumeroNaoInteiro() throws Exception {
        GeminiApiExcecao e = rejeita("{\"blocos\":[{\"tipo\":\"estrofe\",\"numeroEstrofe\":\"2\","
                + "\"linhas\":[]}]}");
        assertTrue(e.getMessage().contains("numeroEstrofe"));
    }

    @Test
    void aceitaDuvidasESemanticaCompleta() {
        assertDoesNotThrow(() -> validar("{\"blocos\":[{\"tipo\":\"estrofe\",\"numeroEstrofe\":2,"
                + "\"italico\":false,\"continuacaoDeBlocoAnterior\":true,"
                + "\"linhas\":[{\"texto\":\"[?] kuanza\",\"recuo\":2}],"
                + "\"duvidas\":[{\"linha\":0,\"trecho\":\"[?]\",\"motivo\":\"ilegível\"}]}],"
                + "\"cabecalhoPagina\":null,\"numeroPaginaImpresso\":395}"));
    }
}
