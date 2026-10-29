package com.ravunana.hinario.estrutura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PRD Fase 5: hinos, coro proposto (5.7.6), ligação (5.7.4), incompletas. */
class EstruturadorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Estruturador estruturador = new Estruturador();

    private ObjectNode resposta() {
        ObjectNode raiz = MAPPER.createObjectNode();
        raiz.put("numeroPaginaImpresso", 1);
        raiz.putNull("cabecalhoPagina");
        raiz.putArray("blocos");
        return raiz;
    }

    private ArrayNode blocos(JsonNode raiz) {
        return (ArrayNode) raiz.get("blocos");
    }

    private void titulo(JsonNode raiz, int hino, String titulo) {
        ObjectNode bloco = blocos(raiz).addObject();
        bloco.put("tipo", "titulo_hino");
        bloco.put("numeroHino", hino);
        bloco.put("titulo", titulo);
        bloco.putNull("numeroEstrofe");
        bloco.put("italico", false);
        bloco.put("continuacaoDeBlocoAnterior", false);
        bloco.putArray("linhas");
        bloco.putArray("duvidas");
    }

    private void estrofe(JsonNode raiz, int n, boolean continuacao, String... linhas) {
        ObjectNode bloco = blocos(raiz).addObject();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        if (n < 0) {
            bloco.putNull("numeroEstrofe");
        } else {
            bloco.put("numeroEstrofe", n);
        }
        bloco.put("italico", false);
        bloco.put("continuacaoDeBlocoAnterior", continuacao);
        ArrayNode array = bloco.putArray("linhas");
        for (String linha : linhas) {
            array.addObject().put("texto", linha).put("recuo", 0);
        }
        bloco.putArray("duvidas");
    }

    private void semNumero(JsonNode raiz, boolean italico, boolean recuo, String... linhas) {
        ObjectNode bloco = blocos(raiz).addObject();
        bloco.put("tipo", "bloco_sem_numero");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.putNull("numeroEstrofe");
        bloco.put("italico", italico);
        bloco.put("continuacaoDeBlocoAnterior", false);
        ArrayNode array = bloco.putArray("linhas");
        for (String linha : linhas) {
            array.addObject().put("texto", linha).put("recuo", recuo ? 1 : 0);
        }
        bloco.putArray("duvidas");
    }

    @Test
    void montaHinoComTituloEEstrofes() {
        JsonNode raiz = resposta();
        titulo(raiz, 42, "NGANA JEZU");
        estrofe(raiz, 1, false, "Ngana Jezu, nga mu zola,", "Muxima diami dise.");
        estrofe(raiz, 2, false, "Nga mu zola, Jesus!");

        EstruturaPagina estrutura = estruturador.construir(raiz, null, false);

        assertEquals(3, estrutura.blocos().size());
        assertEquals("titulo_hino", estrutura.blocos().get(0).path("tipo").asText());
        assertEquals("NGANA JEZU", estrutura.blocos().get(0).path("titulo").asText());
        assertEquals("estrofe", estrutura.blocos().get(1).path("tipo").asText());
        assertEquals(1, estrutura.blocos().get(1).path("numeroEstrofe").asInt());
        // texto verbatim, sem alterações
        assertEquals("Ngana Jezu, nga mu zola,",
                estrutura.blocos().get(1).path("linhas").get(0).path("texto").asText());
        assertEquals(new ContextoHino(42, "NGANA JEZU", 2), estrutura.hinoAberto());
        assertFalse(estrutura.continuaDoAnterior());
        assertFalse(estrutura.blocos().get(1).path("incompleta").asBoolean());
    }

    @Test
    void blocoSemNumeroEntreEstrofesComItalicoViraCoroProposto() {
        JsonNode raiz = resposta();
        titulo(raiz, 42, "T");
        estrofe(raiz, 1, false, "linha um");
        semNumero(raiz, true, false, "Nga mu zola, Jesus!");
        estrofe(raiz, 2, false, "linha dois");

        EstruturaPagina estrutura = estruturador.construir(raiz, null, false);

        JsonNode coro = estrutura.blocos().get(2);
        assertEquals("coro", coro.path("tipo").asText());
        assertTrue(coro.path("possivelCoro").asBoolean());
    }

    @Test
    void blocoSemNumeroSemItalicoNemRecuoFicaSemClassificar() {
        JsonNode raiz = resposta();
        titulo(raiz, 42, "T");
        estrofe(raiz, 1, false, "linha um");
        semNumero(raiz, false, false, "texto solto");
        estrofe(raiz, 2, false, "linha dois");

        JsonNode bloco = estruturador.construir(raiz, null, false).blocos().get(2);
        assertEquals("bloco_sem_numero", bloco.path("tipo").asText());
        assertFalse(bloco.path("possivelCoro").asBoolean());
    }

    @Test
    void recuoTambemPropoeCoro() {
        JsonNode raiz = resposta();
        estrofe(raiz, 1, false, "linha um");
        semNumero(raiz, false, true, "Nga mu zola!");
        estrofe(raiz, 2, false, "linha dois");

        assertEquals("coro", estruturador.construir(raiz, null, false).blocos().get(1).path("tipo").asText());
    }

    @Test
    void continuacaoValidaJuntaSemConfirmar() {
        JsonNode raiz = resposta();
        estrofe(raiz, 2, true, "continuação da estrofe 1");
        estrofe(raiz, 3, false, "estância nova");

        EstruturaPagina estrutura = estruturador.construir(raiz,
                new ContextoHino(42, "NGANA JEZU", 1), false);

        JsonNode primeira = estrutura.blocos().get(0);
        assertTrue(estrutura.continuaDoAnterior());
        assertEquals(42, primeira.path("numeroHino").asInt());
        assertFalse(primeira.path("ligacao").path("confirmar").asBoolean());
        assertEquals(42, estrutura.hinoAberto().numeroHino().intValue());
        assertEquals(3, estrutura.hinoAberto().ultimaEstrofe().intValue());
    }

    @Test
    void buracoNaSequenciaPedeConfirmacaoEmVezDeJuntar() {
        JsonNode raiz = resposta();
        estrofe(raiz, 3, true, "abre em estrofe 3");

        JsonNode ligacao = estruturador.construir(raiz,
                new ContextoHino(42, "T", 1), false).blocos().get(0).path("ligacao");

        assertTrue(ligacao.path("confirmar").asBoolean());
        assertEquals(2, ligacao.path("esperado").asInt());
        assertEquals(3, ligacao.path("encontrado").asInt());
        assertEquals(42, ligacao.path("hinoAnterior").asInt());
    }

    @Test
    void continuacaoSemContextoTambemPedeConfirmacao() {
        JsonNode raiz = resposta();
        estrofe(raiz, 1, true, "abre a meio");

        JsonNode ligacao = estruturador.construir(raiz, null, false).blocos().get(0).path("ligacao");

        assertTrue(ligacao.path("confirmar").asBoolean());
        assertTrue(ligacao.path("esperado").isNull());
    }

    @Test
    void buracoDentroDaPaginaFicaSuspeito() {
        JsonNode raiz = resposta();
        titulo(raiz, 42, "T");
        estrofe(raiz, 1, false, "a");
        estrofe(raiz, 3, false, "b");

        EstruturaPagina estrutura = estruturador.construir(raiz, null, false);

        assertFalse(estrutura.blocos().get(1).path("sequenciaSuspeita").asBoolean());
        assertTrue(estrutura.blocos().get(2).path("sequenciaSuspeita").asBoolean());
    }

    @Test
    void doisHinosNaMesmaPagina() {
        JsonNode raiz = resposta();
        titulo(raiz, 42, "UM");
        estrofe(raiz, 1, false, "a");
        titulo(raiz, 43, "DOIS");
        estrofe(raiz, 1, false, "b");

        EstruturaPagina estrutura = estruturador.construir(raiz, null, false);

        assertEquals(4, estrutura.blocos().size());
        assertEquals("UM", estrutura.blocos().get(0).path("titulo").asText());
        assertEquals(43, estrutura.blocos().get(2).path("numeroHino").asInt());
        assertEquals(43, estrutura.hinoAberto().numeroHino().intValue());
        assertEquals(1, estrutura.hinoAberto().ultimaEstrofe().intValue());
    }

    @Test
    void proximoQueContinuaMarcaUltimaEstrofeIncompleta() {
        JsonNode raiz = resposta();
        estrofe(raiz, 1, false, "a");
        estrofe(raiz, 2, false, "b");

        EstruturaPagina estrutura = estruturador.construir(raiz, null, true);

        assertFalse(estrutura.blocos().get(0).path("incompleta").asBoolean());
        assertTrue(estrutura.blocos().get(1).path("incompleta").asBoolean());
    }

    @Test
    void abrirContextoUsaOUltimoHinoEEstrofe() {
        JsonNode raiz = resposta();
        titulo(raiz, 42, "UM");
        estrofe(raiz, 1, false, "a");
        semNumero(raiz, true, false, "coro");
        estrofe(raiz, 2, false, "b");

        ContextoHino ctx = estruturador.abrirContexto(raiz);

        assertEquals(new ContextoHino(42, "UM", 2), ctx);
        assertNull(estruturador.abrirContexto(resposta()));
    }
}
