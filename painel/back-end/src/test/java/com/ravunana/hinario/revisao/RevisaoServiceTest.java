package com.ravunana.hinario.revisao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.estrutura.EstruturaService;
import com.ravunana.hinario.estrutura.Estruturador;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PRD Fase 6 — `PUT /linhas` e `POST /confirmar` (aceitação: texto preservado). */
class RevisaoServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private DataFiles dataFiles;
    private PaginaService paginas;
    private EstruturaService estruturas;
    private RevisaoService revisao;

    @BeforeEach
    void preparar() {
        dataFiles = new DataFiles(temp.toString());
        paginas = new PaginaService(dataFiles, new ImagemProcessadora());
        estruturas = new EstruturaService(new Estruturador(), paginas);
        revisao = new RevisaoService(paginas, dataFiles);
    }

    /** Página estruturada com uma estrofe de duas linhas lidas pelo Gemini. */
    private int estruturar(int numero) throws Exception {
        ObjectNode resposta = MAPPER.createObjectNode();
        ObjectNode bloco = resposta.putArray("blocos").addObject();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.put("numeroEstrofe", 1);
        bloco.put("italico", false);
        bloco.put("continuacaoDeBlocoAnterior", false);
        ArrayNode linhas = bloco.putArray("linhas");
        linhas.addObject().put("texto", "Ngana Jezu, nga mu zola,").put("recuo", 0);
        linhas.addObject().put("texto", "Muxima diami dise").put("recuo", 1);
        bloco.putArray("duvidas");

        ObjectNode leituras = MAPPER.createObjectNode();
        leituras.putObject("gemini").set("resposta", resposta);
        paginas.atualizar(new Pagina(numero, "kmb",
                "imagens/original/%04d.jpg".formatted(numero),
                "imagens/processada/%04d.jpg".formatted(numero),
                "sha256:" + numero, EstadoPagina.COMPARADA,
                "%04d.jpg".formatted(numero), "2026-10-28T10:00:00Z",
                100, 80, leituras, null, null));
        estruturas.estruturar(numero);
        return numero;
    }

    private ArrayNode blocosCorrigidos(String primeiraLinha) {
        ObjectNode bloco = MAPPER.createObjectNode();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.put("numeroEstrofe", 1);
        ArrayNode linhas = bloco.putArray("linhas");
        linhas.addObject().put("texto", primeiraLinha).put("recuo", 0);
        linhas.addObject().put("texto", "Muxima diami dise").put("recuo", 1);
        ArrayNode blocos = MAPPER.createArrayNode();
        blocos.add(bloco);
        return blocos;
    }

    @Test
    void guardaCorrecoesENormalizaParaNfc() throws Exception {
        int numero = estruturar(1);
        // "e" + combining acute (NFD) deve sair NFC
        String nfd = "Ngana Jezu, nga mu zola\u0301";

        Pagina gravada = revisao.guardarLinhas(numero, blocosCorrigidos(nfd));

        assertEquals(EstadoPagina.EM_REVISAO, gravada.estado());
        String texto = gravada.blocos().get(0).path("linhas").get(0).path("texto").asText();
        assertTrue(texto.endsWith("zolá"), "devia estar em NFC: " + texto);
        assertFalse(texto.contains("\u0301"), "combining mark residual");
    }

    @Test
    void leituraOriginalFicaIntactaDepoisDeCorrigir() throws Exception {
        int numero = estruturar(2);

        Pagina gravada = revisao.guardarLinhas(numero, blocosCorrigidos("TEXTO CORRIGIDO"));

        String original = gravada.leituras().path("gemini").path("resposta")
                .path("blocos").get(0).path("linhas").get(0).path("texto").asText();
        assertEquals("Ngana Jezu, nga mu zola,", original);
        assertEquals("TEXTO CORRIGIDO",
                gravada.blocos().get(0).path("linhas").get(0).path("texto").asText());
    }

    @Test
    void confirmarMarcaLinhasComoRevisto() throws Exception {
        int numero = estruturar(3);
        revisao.guardarLinhas(numero, blocosCorrigidos("corrigida"));

        Pagina confirmada = revisao.confirmar(numero);

        assertEquals(EstadoPagina.CONFIRMADA, confirmada.estado());
        assertTrue(confirmada.blocos().get(0).path("linhas").get(0).path("revisto").asBoolean());
        assertTrue(confirmada.blocos().get(0).path("linhas").get(1).path("revisto").asBoolean());
    }

    @Test
    void guardarParamsDeEstadoAnterioresDao409() throws Exception {
        paginas.atualizar(new Pagina(4, "kmb", "imagens/original/0004.jpg",
                "imagens/processada/0004.jpg", "sha256:4", EstadoPagina.IMPORTADA,
                "0004.jpg", "2026-10-28T10:00:00Z", 100, 80, null, null, null));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> revisao.guardarLinhas(4, blocosCorrigidos("x")));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void confirmarSemEstruturacaoDá409() throws Exception {
        paginas.atualizar(new Pagina(5, "kmb", "imagens/original/0005.jpg",
                "imagens/processada/0005.jpg", "sha256:5", EstadoPagina.EM_REVISAO,
                "0005.jpg", "2026-10-28T10:00:00Z", 100, 80, null, null, null));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> revisao.confirmar(5));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void recuoForaDoIntervaloDá400() throws Exception {
        int numero = estruturar(6);
        ArrayNode blocos = blocosCorrigidos("x");
        ((ObjectNode) blocos.get(0).path("linhas").get(1)).put("recuo", 5);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> revisao.guardarLinhas(numero, blocos));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void tipoDeBlocoInvalidoDá400() throws Exception {
        int numero = estruturar(7);
        ArrayNode blocos = blocosCorrigidos("x");
        ((ObjectNode) blocos.get(0)).put("tipo", "refrao");

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> revisao.guardarLinhas(numero, blocos));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void regravarAposConfirmarPermitido() throws Exception {
        int numero = estruturar(8);
        revisao.guardarLinhas(numero, blocosCorrigidos("primeira"));
        revisao.confirmar(numero);

        Pagina outra = revisao.guardarLinhas(numero, blocosCorrigidos("segunda"));
        assertEquals(EstadoPagina.EM_REVISAO, outra.estado());
    }
}
