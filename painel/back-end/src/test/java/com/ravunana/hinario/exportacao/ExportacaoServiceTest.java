package com.ravunana.hinario.exportacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.model.Estrofe;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.ImagemProcessadora;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import com.ravunana.hinario.services.EstrofeService;
import com.ravunana.hinario.services.HinoService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRD Fase 7 — aceitação: o par 394/395 confirmado exporta o hino 42 completo
 * (estrofe 1 junta entre páginas) com o UUID do JSON actual; o JSON gerado
 * valida e só tem linhas revistas.
 */
class ExportacaoServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private DataFiles dataFiles;
    private PaginaService paginas;
    private HinoService hinosAntigos;
    private ExportacaoService exportacao;

    private final UUID idExistente = UUID.fromString("68489a40-16eb-49a0-8e80-ad93dd5606da");

    @BeforeEach
    void preparar() throws Exception {
        dataFiles = new DataFiles(temp.toString());
        gravarHinarioAntigo();
        hinosAntigos = new HinoService(dataFiles);
        paginas = new PaginaService(dataFiles, new ImagemProcessadora());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        exportacao = new ExportacaoService(paginas, hinosAntigos,
                new ExportacaoValidador(validator), dataFiles);
    }

    private void gravarHinarioAntigo() throws Exception {
        ObjectNode hino = MAPPER.createObjectNode();
        hino.put("id", idExistente.toString());
        hino.put("titulo", "NGANA JEZU");
        hino.put("pagina", 394);
        hino.put("numero", 42);
        hino.put("autor", "IERA");
        hino.put("dataCriacao", "2024-03-21");
        hino.put("dataActualizacao", "2024-06-29");
        hino.put("comentario", "sem comentário");
        hino.put("isFavorito", false);
        hino.put("lingua", "KINBUMDU");
        ArrayNode hinos = MAPPER.createArrayNode();
        hinos.add(hino);
        dataFiles.escreverJson(dataFiles.resolve("hinario.json"), hinos);
        dataFiles.escreverTexto(dataFiles.resolve("estrofes.json"), "[]");
    }

    /** Uma página `confirmada` com blocos da revisão (linhas revistas). */
    private void gravarConfirmada(int numero, ArrayNode blocos) throws Exception {
        Pagina pagina = new Pagina(numero, "kmb",
                "imagens/original/%04d.jpg".formatted(numero),
                "imagens/processada/%04d.jpg".formatted(numero),
                "sha256:" + numero, EstadoPagina.CONFIRMADA,
                "%04d.jpg".formatted(numero), "2026-10-28T10:00:00Z",
                100, 80, MAPPER.createObjectNode(), null, blocos);
        paginas.atualizar(pagina);
    }

    private ObjectNode titulo(int hino, String titulo) {
        ObjectNode bloco = MAPPER.createObjectNode();
        bloco.put("tipo", "titulo_hino");
        bloco.put("numeroHino", hino);
        bloco.put("titulo", titulo);
        bloco.putNull("numeroEstrofe");
        bloco.putArray("linhas");
        bloco.putArray("duvidas");
        return bloco;
    }

    private ObjectNode estrofe(int n, boolean continuacao, boolean revisto, String... linhas) {
        ObjectNode bloco = MAPPER.createObjectNode();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.put("numeroEstrofe", n);
        bloco.put("continuacao", continuacao);
        bloco.put("incompleta", false);
        ArrayNode array = bloco.putArray("linhas");
        for (String linha : linhas) {
            ObjectNode registo = array.addObject();
            registo.put("texto", linha);
            registo.put("recuo", 0);
            registo.put("revisto", revisto);
        }
        bloco.putArray("duvidas");
        return bloco;
    }

    /** Estrofe com linhas revistas e por rever, para testar o filtro. */
    private ObjectNode estrofeMista(int n, String revista, String porRever) {
        ObjectNode bloco = MAPPER.createObjectNode();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.put("numeroEstrofe", n);
        bloco.put("continuacao", false);
        bloco.put("incompleta", false);
        ArrayNode array = bloco.putArray("linhas");
        ObjectNode l1 = array.addObject();
        l1.put("texto", revista);
        l1.put("recuo", 0);
        l1.put("revisto", true);
        ObjectNode l2 = array.addObject();
        l2.put("texto", porRever);
        l2.put("recuo", 0);
        l2.put("revisto", false);
        bloco.putArray("duvidas");
        return bloco;
    }

    private ArrayNode blocos(ObjectNode... blocos) {
        ArrayNode array = MAPPER.createArrayNode();
        for (ObjectNode bloco : blocos) {
            array.add(bloco);
        }
        return array;
    }

    @Test
    void par394e395ExportaHinoCompletoComIdDoJsonActual() throws Exception {
        // 394: hino 42, estrofe 1 completa, estrofe 2 incompleta (última da página)
        ObjectNode e2 = estrofe(2, false, true, "Nga mu zola, Jesus!", "Muxima diami dise.");
        e2.put("incompleta", true);
        gravarConfirmada(394, blocos(titulo(42, "NGANA JEZU"),
                estrofe(1, false, true, "Ngana Jezu, nga mu zola,"), e2));
        // 395: continuação da estrofe 2 (com numeroHino, como o Estruturador faz)
        ObjectNode continua = estrofe(2, true, true, "A bula kwizuva kwami.");
        continua.put("numeroHino", 42);
        gravarConfirmada(395, blocos(continua,
                estrofe(3, false, true, "A kiza bika diaku.")));

        List<ExportacaoService.ResultadoExportacao> resultados = exportacao.exportar("kmb");

        assertEquals(1, resultados.size());
        ExportacaoService.ResultadoExportacao resultado = resultados.get(0);
        assertTrue(resultado.gerado(), "erros: " + resultado.erros());
        assertTrue(resultado.erros().isEmpty());
        assertEquals(1, resultado.hinos());

        JsonNode ficheiro = MAPPER.readTree(
                Files.readString(dataFiles.resolve("export/hinario-kmb.json")));
        assertEquals("kmb", ficheiro.path("id").asText());
        JsonNode hino = ficheiro.path("hinos").get(0);
        assertEquals(idExistente.toString(), hino.path("id").asText());
        assertEquals(42, hino.path("numero").asInt());
        assertEquals("NGANA JEZU", hino.path("titulo").asText());

        // estrofe 2 junta entre páginas: 3 linhas numa só
        ArrayNode blocosExportados = (ArrayNode) hino.path("blocos");
        assertEquals(3, blocosExportados.size());
        assertEquals("estrofe", blocosExportados.get(0).path("tipo").asText());
        assertEquals(1, blocosExportados.get(0).path("numero").asInt());
        assertEquals(2, blocosExportados.get(1).path("numero").asInt());
        assertEquals(3, blocosExportados.get(1).path("linhas").size());
        assertEquals("A bula kwizuva kwami.",
                blocosExportados.get(1).path("linhas").get(2).path("texto").asText());
        // estrofes 1, 2, 3 contínuas
        assertEquals(3, blocosExportados.get(2).path("numero").asInt());
    }

    @Test
    void soLinhasRevistasSaoExportadas() throws Exception {
        gravarConfirmada(10, blocos(titulo(99, "TESTE"),
                estrofeMista(1, "linha revista", "linha por rever")));

        ExportacaoService.ResultadoExportacao resultado = exportacao.exportar("kmb").get(0);
        assertTrue(resultado.gerado(), "erros: " + resultado.erros());

        JsonNode ficheiro = MAPPER.readTree(
                Files.readString(dataFiles.resolve("export/hinario-kmb.json")));
        ArrayNode linhas = (ArrayNode) ficheiro.path("hinos").get(0)
                .path("blocos").get(0).path("linhas");
        assertEquals(1, linhas.size());
        assertEquals("linha revista", linhas.get(0).path("texto").asText());
    }

    @Test
    void blocoSemNumeroPorClassificarBloqueia() throws Exception {
        ObjectNode solto = estrofe(0, false, true, "texto solto");
        solto.put("tipo", "bloco_sem_numero");
        gravarConfirmada(11, blocos(titulo(98, "X"), solto));

        ExportacaoService.ResultadoExportacao resultado = exportacao.exportar("kmb").get(0);

        assertFalse(resultado.gerado());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("por classificar")),
                "erros: " + resultado.erros());
    }

    @Test
    void estrofeIncompletaNoFimBloqueia() throws Exception {
        ObjectNode e1 = estrofe(1, false, true, "linha");
        e1.put("incompleta", true);
        gravarConfirmada(12, blocos(titulo(97, "Y"), e1));

        ExportacaoService.ResultadoExportacao resultado = exportacao.exportar("kmb").get(0);

        assertFalse(resultado.gerado());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("incompleta")),
                "erros: " + resultado.erros());
    }

    @Test
    void semPaginasConfirmadasDizONada() {
        ExportacaoService.ResultadoExportacao resultado = exportacao.exportar("kkr").get(0);

        assertFalse(resultado.gerado());
        assertTrue(resultado.erros().get(0).contains("não há páginas confirmadas"));
    }

    @Test
    void espacosABloquearAparecemComoErro() throws Exception {
        gravarConfirmada(13, blocos(titulo(96, "Z"),
                estrofe(1, false, true, "linha  com  espaços")));

        ExportacaoService.ResultadoExportacao resultado = exportacao.exportar("kmb").get(0);

        assertFalse(resultado.gerado());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("espaços a mais")),
                "erros: " + resultado.erros());
    }

    @Test
    void semCodigoExportaTodasAsLinguasComConfirmadas() throws Exception {
        gravarConfirmada(14, blocos(titulo(95, "W"), estrofe(1, false, true, "ok")));

        List<ExportacaoService.ResultadoExportacao> resultados = exportacao.exportar(null);

        assertEquals(Linguas.values().length, resultados.size());
        assertTrue(resultados.stream().filter(ExportacaoService.ResultadoExportacao::gerado).count() >= 1);
    }

    @Test
    void hinoMontadoDevolveLeituraParaComparar() throws Exception {
        gravarConfirmada(15, blocos(titulo(42, "NGANA JEZU"),
                estrofe(1, false, true, "Ngana Jezu, nga mu zola,")));

        HinoNovo montado = exportacao.hinoMontado("kmb", 42).orElseThrow();

        assertEquals(42, montado.numero());
        assertEquals(1, montado.blocos().size());
        assertEquals("Ngana Jezu, nga mu zola,",
                montado.blocos().get(0).linhas().get(0).texto());
        assertTrue(exportacao.hinoMontado("kmb", 77).isEmpty());
    }
}
