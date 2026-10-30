package com.ravunana.hinario.comparacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.exportacao.ExportacaoService;
import com.ravunana.hinario.exportacao.ExportacaoValidador;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.ImagemProcessadora;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import com.ravunana.hinario.services.EstrofeService;
import com.ravunana.hinario.services.HinoService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PRD Fase 7 — {@code GET /comparar/{hinoId}}: a leitura nova contra o JSON
 * actual, com diferenças classificadas.
 */
class ComparacaoHinoServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private DataFiles dataFiles;
    private PaginaService paginas;
    private ComparacaoHinoService comparacao;

    private final UUID idHino = UUID.fromString("68489a40-16eb-49a0-8e80-ad93dd5606da");

    @BeforeEach
    void preparar() throws Exception {
        dataFiles = new DataFiles(temp.toString());
        gravarJsonActual();
        HinoService hinos = new HinoService(dataFiles);
        EstrofeService estrofes = new EstrofeService(dataFiles);
        paginas = new PaginaService(dataFiles, new ImagemProcessadora());
        ExportacaoService exportacao = new ExportacaoService(paginas, hinos,
                new ExportacaoValidador(Validation.buildDefaultValidatorFactory().getValidator()),
                dataFiles);
        comparacao = new ComparacaoHinoService(hinos, estrofes, exportacao);
    }

    /** hinario.json com o hino 42 + estrofes.json com estrofe 1 e coro. */
    private void gravarJsonActual() throws Exception {
        ObjectNode hinoJson = MAPPER.createObjectNode();
        hinoJson.put("id", idHino.toString());
        hinoJson.put("titulo", "NGANA JEZU");
        hinoJson.put("pagina", 394);
        hinoJson.put("numero", 42);
        hinoJson.put("autor", "IERA");
        hinoJson.put("dataCriacao", "2024-03-21");
        hinoJson.put("dataActualizacao", "2024-06-29");
        hinoJson.put("comentario", "sem comentário");
        hinoJson.put("isFavorito", false);
        hinoJson.put("lingua", "KINBUMDU");
        ArrayNode hinos = MAPPER.createArrayNode();
        hinos.add(hinoJson);
        dataFiles.escreverJson(dataFiles.resolve("hinario.json"), hinos);

        ArrayNode estrofes = MAPPER.createArrayNode();
        estrofes.add(estrofeJson(UUID.randomUUID(), 1,
                "Ngana Jezu, nga mu zola,\nMuxîma diami dise.", false, hinoJson));
        estrofes.add(estrofeJson(UUID.randomUUID(), 0,
                "Nga mu zola, Jesus!\nNga mu zola, Jesus!", true, hinoJson));
        dataFiles.escreverJson(dataFiles.resolve("estrofes.json"), estrofes);
    }

    private ObjectNode estrofeJson(UUID id, long numero, String descricao, boolean refrao,
                                   ObjectNode hino) {
        ObjectNode estrofe = MAPPER.createObjectNode();
        estrofe.put("id", id.toString());
        estrofe.put("numero", numero);
        estrofe.put("descricao", descricao);
        estrofe.put("refrao", refrao);
        estrofe.set("hino", hino.deepCopy());
        return estrofe;
    }

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

    private ObjectNode bloco(String tipo, Integer numeroEstrofe, boolean revisto, String... linhas) {
        ObjectNode bloco = MAPPER.createObjectNode();
        bloco.put("tipo", tipo);
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        if (numeroEstrofe == null) {
            bloco.putNull("numeroEstrofe");
        } else {
            bloco.put("numeroEstrofe", numeroEstrofe);
        }
        bloco.put("continuacao", false);
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

    private ArrayNode blocos(ObjectNode... blocos) {
        ArrayNode array = MAPPER.createArrayNode();
        for (ObjectNode bloco : blocos) {
            array.add(bloco);
        }
        return array;
    }

    @Test
    void diferencaDeAcentoENaoDivergente() throws Exception {
        // a leitura perde o acento de "Muxîma" — só_acento, nunca divergente
        gravarConfirmada(394, blocos(titulo(42, "NGANA JEZU"),
                bloco("estrofe", 1, true, "Ngana Jezu, nga mu zola,", "Muxima diami dise."),
                bloco("coro", null, true, "Nga mu zola, Jesus!", "Nga mu zola, Jesus!")));

        ComparacaoHinoService.ComparacaoHino resultado = comparacao.comparar(idHino);

        assertTrue(resultado.temLeituraNova());
        assertTrue(resultado.diferencas().stream().anyMatch(d -> "so_acento".equals(d.tipo())),
                "diferenças: " + resultado.diferencas());
        assertFalse(resultado.diferencas().stream().anyMatch(d -> "divergente".equals(d.tipo())));
        // coro do JSON actual vira bloco coro no lado novo
        assertTrue(resultado.nova().stream().anyMatch(b -> b.tipo().equals("coro")));
        // as duas linhas iguais do coro contam
        assertTrue(resultado.linhasIguais() >= 2, "iguais: " + resultado.linhasIguais());
    }

    @Test
    void linhaSoDoLadoNovoESinalizada() throws Exception {
        // leitura tem uma linha extra que o JSON actual não tem
        gravarConfirmada(394, blocos(titulo(42, "NGANA JEZU"),
                bloco("estrofe", 1, true, "Ngana Jezu, nga mu zola,",
                        "Muxima diami dise.", "Linha extra só na leitura."),
                bloco("coro", null, true, "Nga mu zola, Jesus!", "Nga mu zola, Jesus!")));

        ComparacaoHinoService.ComparacaoHino resultado = comparacao.comparar(idHino);

        assertTrue(resultado.diferencas().stream()
                        .anyMatch(d -> "so_no_novo".equals(d.tipo()) && d.nova().contains("extra")),
                "diferenças: " + resultado.diferencas());
    }

    @Test
    void semLeituraNovaDevolveVazioMasMostraActual() {
        ComparacaoHinoService.ComparacaoHino resultado = comparacao.comparar(idHino);

        assertFalse(resultado.temLeituraNova());
        assertTrue(resultado.nova().isEmpty());
        // todas as linhas actuais ficam "so_na_leitura" (2 da estrofe + 2 do coro)
        assertEquals(4, resultado.diferencas().size(),
                "diferenças: " + resultado.diferencas());
        assertTrue(resultado.diferencas().stream().allMatch(d -> "so_na_leitura".equals(d.tipo())));
    }

    @Test
    void hinoInexistenteDiz404() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> comparacao.comparar(UUID.randomUUID()));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void divergenciaDeTextoENaoIgnorada() throws Exception {
        // "Nzambi" no lugar de "nga": parecido o suficiente para emparelhar,
        // mas diferente em letras — divergente
        gravarConfirmada(394, blocos(titulo(42, "NGANA JEZU"),
                bloco("estrofe", 1, true, "Ngana Jezu, Nzambi mu zola,", "Muxima diami dise."),
                bloco("coro", null, true, "Nga mu zola, Jesus!", "Nga mu zola, Jesus!")));

        ComparacaoHinoService.ComparacaoHino resultado = comparacao.comparar(idHino);

        assertTrue(resultado.diferencas().stream().anyMatch(d -> "divergente".equals(d.tipo())),
                "diferenças: " + resultado.diferencas());
    }

    @Test
    void soPontuacaoNaoDiverge() throws Exception {
        // só falta a pontuação final — o acento de "Muxîma" está lá dos dois lados
        gravarConfirmada(394, blocos(titulo(42, "NGANA JEZU"),
                bloco("estrofe", 1, true, "Ngana Jezu, nga mu zola", "Muxîma diami dise"),
                bloco("coro", null, true, "Nga mu zola Jesus", "Nga mu zola Jesus")));

        List<ComparacaoHinoService.LinhaComparada> difs =
                comparacao.comparar(idHino).diferencas();

        assertTrue(difs.stream().allMatch(d -> !"divergente".equals(d.tipo())),
                "diferenças: " + difs);
        assertTrue(difs.stream().anyMatch(d -> "so_pontuacao".equals(d.tipo())),
                "diferenças: " + difs);
    }

    @Test
    void blocoActualCarregaReferenciaDeLinha() throws Exception {
        gravarConfirmada(394, blocos(titulo(42, "NGANA JEZU"),
                bloco("estrofe", 1, true, "Ngana Jezu, Nzambi mu zola,", "Muxima diami dise."),
                bloco("coro", null, true, "Nga mu zola, Jesus!", "Nga mu zola, Jesus!")));

        ComparacaoHinoService.ComparacaoHino resultado = comparacao.comparar(idHino);

        ComparacaoHinoService.LinhaComparada dif = resultado.diferencas().stream()
                .filter(d -> "divergente".equals(d.tipo())).findFirst().orElseThrow();
        assertEquals(0, dif.blocoActual());
        assertEquals(0, dif.linhaActual());
        assertEquals(0, dif.blocoNova());
        assertEquals(0, dif.linhaNova());
    }
}
