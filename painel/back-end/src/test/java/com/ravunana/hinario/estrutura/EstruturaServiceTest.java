package com.ravunana.hinario.estrutura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.ImagemProcessadora;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRD Fase 5 — aceitação: o par 394/395 gera um hino 42 completo e o hino 43
 * ligado às páginas (395 a abrir na estrofe 2); casos duvidosos ficam marcados,
 * nunca juntos por suposição.
 */
class EstruturaServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private DataFiles dataFiles;
    private PaginaService paginas;
    private EstruturaService estruturas;

    @BeforeEach
    void preparar() {
        dataFiles = new DataFiles(temp.toString());
        paginas = new PaginaService(dataFiles, new ImagemProcessadora());
        estruturas = new EstruturaService(new Estruturador(), paginas);
    }

    private ObjectNode resposta() {
        ObjectNode raiz = MAPPER.createObjectNode();
        raiz.putArray("blocos");
        return raiz;
    }

    private void titulo(ObjectNode raiz, int hino, String titulo) {
        ObjectNode bloco = ((ArrayNode) raiz.get("blocos")).addObject();
        bloco.put("tipo", "titulo_hino");
        bloco.put("numeroHino", hino);
        bloco.put("titulo", titulo);
        bloco.putNull("numeroEstrofe");
        bloco.put("italico", false);
        bloco.put("continuacaoDeBlocoAnterior", false);
        bloco.putArray("linhas");
        bloco.putArray("duvidas");
    }

    private void estrofe(ObjectNode raiz, int n, boolean continuacao, String... linhas) {
        ObjectNode bloco = ((ArrayNode) raiz.get("blocos")).addObject();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.put("numeroEstrofe", n);
        bloco.put("italico", false);
        bloco.put("continuacaoDeBlocoAnterior", continuacao);
        ArrayNode array = bloco.putArray("linhas");
        for (String linha : linhas) {
            array.addObject().put("texto", linha).put("recuo", 0);
        }
        bloco.putArray("duvidas");
    }

    /** Grava a página directamente em estado `comparada`, com a resposta Gemini. */
    private Pagina gravarComparada(int numero, ObjectNode resposta) throws Exception {
        ObjectNode leituras = MAPPER.createObjectNode();
        leituras.putObject("gemini").set("resposta", resposta);
        Pagina pagina = new Pagina(numero, "kmb",
                "imagens/original/%04d.jpg".formatted(numero),
                "imagens/processada/%04d.jpg".formatted(numero),
                "sha256:" + numero, EstadoPagina.COMPARADA,
                "%04d.jpg".formatted(numero), "2026-10-28T10:00:00Z",
                100, 80, leituras, null, null);
        paginas.atualizar(pagina);
        return pagina;
    }

    @Test
    void par394e395GeraHinoCompletoELigacaoValida() throws Exception {
        // 394: hino 42 completo + início do hino 43 (estrofe 1)
        ObjectNode r394 = resposta();
        titulo(r394, 42, "NGANA JEZU");
        estrofe(r394, 1, false, "Ngana Jezu, nga mu zola,", "Muxima diami dise.");
        estrofe(r394, 2, false, "Nga mu zola, Jesus!");
        estrofe(r394, 3, false, "A bula kwizuva kwami.");
        titulo(r394, 43, "MPIO WA DIENGA");
        estrofe(r394, 1, false, "Mpio wa dienga kwaetu,");
        gravarComparada(394, r394);

        // 395: abre na estrofe 2 do hino 43 (continuação) e fecha a estrofe 3
        ObjectNode r395 = resposta();
        estrofe(r395, 2, true, "kwa Yuda na Simon.");
        estrofe(r395, 3, false, "A kiza bika diaku.");
        gravarComparada(395, r395);

        Pagina p394 = estruturas.estruturar(394);
        Pagina p395 = estruturas.estruturar(395);

        assertEquals(EstadoPagina.ESTRUTURADA, p394.estado());
        assertEquals(EstadoPagina.ESTRUTURADA, p395.estado());

        // a 395, ao ligar-se, marcou a estrofe aberta da 394 como incompleta
        p394 = paginas.obter(394);

        // hino 42 completo numa só página, com as suas estrofes todas
        ArrayNode blocos394 = (ArrayNode) p394.blocos();
        assertEquals("titulo_hino", blocos394.get(0).path("tipo").asText());
        assertEquals(42, blocos394.get(0).path("numeroHino").asInt());
        assertFalseIncompleta(blocos394.get(1));
        assertFalseIncompleta(blocos394.get(2));
        assertFalseIncompleta(blocos394.get(3));
        // estrofe 1 do hino 43 continua em 395 → incompleta
        assertEquals(43, blocos394.get(5).path("numeroHino").asInt());
        assertEquals(1, blocos394.get(5).path("numeroEstrofe").asInt());
        assertTrue(blocos394.get(5).path("incompleta").asBoolean());

        // 395 liga-se ao hino 43 sem confirmação (terminou em 1, abre em 2)
        JsonNode ligacao = p395.blocos().get(0).path("ligacao");
        assertEquals(43, ligacao.path("hinoAnterior").asInt());
        assertEquals(1, ligacao.path("estrofeAnterior").asInt());
        assertEquals(2, ligacao.path("esperado").asInt());
        assertEquals(2, ligacao.path("encontrado").asInt());
        assertTrue(!ligacao.path("confirmar").asBoolean());
        assertTrue(p395.blocos().get(0).path("continuacao").asBoolean());
        assertEquals(43, p395.blocos().get(0).path("numeroHino").asInt());
    }

    @Test
    void buracoEntrePaginasFicaMarcadoParaConfirmar() throws Exception {
        ObjectNode r396 = resposta();
        estrofe(r396, 5, true, "abre longe");
        gravarComparada(396, r396);
        // a 395 não existe neste teste: sem contexto → confirmar
        Pagina p396 = estruturas.estruturar(396);

        assertTrue(p396.blocos().get(0).path("ligacao").path("confirmar").asBoolean());
    }

    @Test
    void marcaIncompletaDaAnteriorQuandoSeguinteContinua() throws Exception {
        ObjectNode r1 = resposta();
        estrofe(r1, 1, false, "a");
        gravarComparada(1, r1);
        ObjectNode r2 = resposta();
        estrofe(r2, 2, true, "b");
        gravarComparada(2, r2);

        estruturas.estruturar(1);
        Pagina p2 = estruturas.estruturar(2);

        assertTrue(!p2.blocos().get(0).path("ligacao").path("confirmar").asBoolean());
        Pagina p1 = paginas.obter(1);
        assertTrue(p1.blocos().get(0).path("incompleta").asBoolean());
        // a página anterior mantém o seu estado
        assertEquals(EstadoPagina.ESTRUTURADA, p1.estado());
    }

    @Test
    void reEstruturarÉPermitido() throws Exception {
        ObjectNode r = resposta();
        estrofe(r, 1, false, "a");
        gravarComparada(7, r);
        estruturas.estruturar(7);

        Pagina outra = estruturas.estruturar(7);
        assertEquals(EstadoPagina.ESTRUTURADA, outra.estado());
    }

    @Test
    void paginaImportadaNaoEstrutura() throws Exception {
        Pagina pagina = new Pagina(9, "kmb", "imagens/original/0009.jpg",
                "imagens/processada/0009.jpg", "sha256:9", EstadoPagina.IMPORTADA,
                "0009.jpg", "2026-10-28T10:00:00Z", 100, 80, null, null, null);
        paginas.atualizar(pagina);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> estruturas.estruturar(9));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void semLeituraDoGeminiNaoEstrutura() throws Exception {
        Pagina pagina = new Pagina(10, "kmb", "imagens/original/0010.jpg",
                "imagens/processada/0010.jpg", "sha256:10", EstadoPagina.LIDA,
                "0010.jpg", "2026-10-28T10:00:00Z", 100, 80, MAPPER.createObjectNode(), null, null);
        paginas.atualizar(pagina);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> estruturas.estruturar(10));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    private static void assertFalseIncompleta(JsonNode bloco) {
        assertTrue(!bloco.path("incompleta").asBoolean(),
                "não devia estar incompleta: " + bloco);
    }
}
