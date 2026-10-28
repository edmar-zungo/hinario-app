package com.ravunana.hinario.migracao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.exportacao.ExportacaoValidador;
import com.ravunana.hinario.exportacao.HinarioNovo;
import com.ravunana.hinario.model.Estrofe;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.services.DataFiles;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigracaoServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private MigracaoService servico(Path raiz) {
        DataFiles dataFiles = new DataFiles(raiz.toString());
        ExportacaoValidador validador = new ExportacaoValidador(
                Validation.buildDefaultValidatorFactory().getValidator());
        return new MigracaoService(dataFiles, validador);
    }

    private void gravarJson(String nome, Object valor) throws IOException {
        Files.writeString(temp.resolve(nome), MAPPER.writeValueAsString(valor));
    }

    private Hino hino(String id, String titulo, long pagina, long numero, String lingua) {
        Hino hino = new Hino();
        hino.setId(UUID.fromString(id));
        hino.setTitulo(titulo);
        hino.setPagina(pagina);
        hino.setNumero(numero);
        hino.setAutor("IERA");
        hino.setLingua(com.ravunana.hinario.enums.Linguas.valueOf(lingua));
        return hino;
    }

    private Estrofe estrofe(String id, long numero, String descricao, boolean refrao, Hino hino) {
        Estrofe estrofe = new Estrofe(UUID.fromString(id), numero, descricao, refrao, hino);
        return estrofe;
    }

    /** Fixtures com os problemas típicos dos dados actuais. */
    private void gravarDadosDeTeste() throws IOException {
        Hino um = hino("11111111-1111-1111-1111-111111111111", "Hino Bom", 10, 1, "KINBUMDU");
        Hino dois = hino("22222222-2222-2222-2222-222222222222", "Hino Três", 11, 3, "KINBUMDU");
        Hino tres = hino("33333333-3333-3333-3333-333333333333", "Português Título", 12, 1, "PORTUGUES");
        Hino copiaA = hino("44444444-4444-4444-4444-444444444444", "Cópia Igual", 20, 5, "KINBUMDU");
        Hino copiaB = hino("55555555-5555-5555-5555-555555555555", "CÓPIA IGUAL", 21, 6, "KINBUMDU");
        Hino orfa = hino("66666666-6666-6666-6666-666666666666", "Só Na Estrofe", 30, 9, "KINBUMDU");

        String linhaLonga = "Moxi diami ng'lasa ia Jezu ".repeat(5).strip();

        gravarJson("hinario.json", List.of(um, dois, tres, copiaA, copiaB));
        gravarJson("estrofes.json", List.of(
                estrofe("aaaaaaaa-0000-0000-0000-000000000001", 1, "Primeira linha\nSegunda linha", false, um),
                estrofe("aaaaaaaa-0000-0000-0000-000000000002", 0, "Coro do hino", true, um),
                estrofe("aaaaaaaa-0000-0000-0000-000000000003", 3, linhaLonga, false, dois),
                estrofe("aaaaaaaa-0000-0000-0000-000000000004", 1, "  Espaço a mais  \n\nFim da estrofe", false, tres),
                estrofe("aaaaaaaa-0000-0000-0000-000000000005", 1, "Texto de uma cópia", false, copiaA),
                estrofe("aaaaaaaa-0000-0000-0000-000000000006", 1, "Texto da cópia divergente", false, copiaB),
                estrofe("aaaaaaaa-0000-0000-0000-000000000007", 9, "Estrofe órfã", false, orfa)));
    }

    @Test
    void migraParaEsquemaNovoSemAlterarTexto() throws IOException {
        gravarDadosDeTeste();

        ResultadoMigracao resultado = servico(temp).migrar();

        assertTrue(resultado.erros().isEmpty(), () -> "erros: " + resultado.erros());
        assertTrue(resultado.textoInalterado(),
                () -> "texto alterado: " + resultado.caracteresAntes() + " -> " + resultado.caracteresDepois());
        assertEquals(1, resultado.estrofesOrfas());
        assertTrue(resultado.ficheirosGerados().containsAll(List.of("hinario-kmb.json", "hinario-por.json")));

        HinarioNovo kmb = MAPPER.readValue(temp.resolve("export/hinario-kmb.json").toFile(), HinarioNovo.class);
        assertEquals("kmb", kmb.id());
        assertEquals(4, kmb.hinos().size(), "hino órfão (só na estrofe.json) não é exportado");

        var primeiro = kmb.hinos().get(0);
        assertEquals("Hino Bom", primeiro.titulo());
        assertEquals(2, primeiro.blocos().size());
        assertEquals("estrofe", primeiro.blocos().get(0).tipo());
        assertEquals("coro", primeiro.blocos().get(1).tipo());
        assertEquals(2, primeiro.blocos().get(0).linhas().size());
        assertEquals("Primeira linha", primeiro.blocos().get(0).linhas().get(0).texto());

        var terceiro = kmb.hinos().stream().filter(h -> h.numero() == 3L).findFirst().orElseThrow();
        assertEquals(1, terceiro.blocos().get(0).linhas().size(), "linha longa sem quebra continua numa só linha");

        HinarioNovo por = MAPPER.readValue(temp.resolve("export/hinario-por.json").toFile(), HinarioNovo.class);
        assertEquals(1, por.hinos().size());
        assertEquals("Português Título", por.hinos().get(0).titulo());
        String textoOriginal = "  Espaço a mais  \n\nFim da estrofe";
        String textoExportado = String.join("\n",
                por.hinos().get(0).blocos().get(0).linhas().stream().map(l -> l.texto()).toList());
        assertEquals(textoOriginal, textoExportado, "o texto deve sair byte a byte igual");
    }

    @Test
    void relatorioCobreTodasAsVerificacoesDoPrd() throws IOException {
        gravarDadosDeTeste();

        ResultadoMigracao resultado = servico(temp).migrar();
        String relatorio = Files.readString(resultado.relatorio());

        assertTrue(relatorio.contains("# Relatório de migração"));
        assertTrue(relatorio.contains("## Integridade do texto"));
        assertTrue(relatorio.contains("Diferença: 0 — nenhum texto alterado pela migração"));
        assertTrue(relatorio.contains("| Estrofes órfãs | 1 |"), relatorio);
        assertTrue(relatorio.contains("## Estrofes órfãs (1)"));
        assertTrue(relatorio.contains("| Hinos sem estrofes | 0 |"));
        assertTrue(relatorio.contains("Nada a assinalar."));
        assertTrue(relatorio.contains("Numeração de hinos"));
        assertTrue(relatorio.contains("buracos (2): 2, 4"), relatorio);
        assertTrue(relatorio.contains("| Sequência de estrofes por hino | 1 |"), relatorio);
        assertTrue(relatorio.contains("buracos: faltam [1, 2]"), relatorio);
        assertTrue(relatorio.contains("Estrofes sem quebra de linha com mais de 80 caracteres (1)"));
        assertTrue(relatorio.contains("Espaços a mais e linhas em branco"));
        assertTrue(relatorio.contains("linha 1 com espaços no início/fim"), relatorio);
        assertTrue(relatorio.contains("linha 2 em branco"), relatorio);
        assertTrue(relatorio.contains("Cópias de hino divergentes (1)"), relatorio);
        assertTrue(relatorio.contains("textos diferentes"), relatorio);
        assertTrue(relatorio.contains("| `kmb` |"), relatorio);
        assertTrue(relatorio.contains("Nenhum. Todos os ficheiros gerados são válidos."));
    }

    @Test
    void segundaCorridaNaoAlteraCaracteres() throws IOException {
        gravarDadosDeTeste();
        MigracaoService servico = servico(temp);

        ResultadoMigracao primeira = servico.migrar();
        long depoisDaPrimeira = primeira.caracteresDepois();
        ResultadoMigracao segunda = servico.migrar();

        assertEquals(primeira.caracteresAntes(), segunda.caracteresAntes());
        assertEquals(depoisDaPrimeira, segunda.caracteresDepois());
        assertTrue(segunda.textoInalterado());
    }

    @Test
    void migraDadosReaisQuandoExistem() throws IOException {
        Path dados = Path.of("data", "hinario.json");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(dados),
                "seed de dados não presente (copie mobile/src/assets/data)");

        DataFiles dataFiles = new DataFiles("");
        ExportacaoValidador validador = new ExportacaoValidador(
                Validation.buildDefaultValidatorFactory().getValidator());
        ResultadoMigracao resultado = new MigracaoService(dataFiles, validador).migrar();

        assertTrue(resultado.erros().isEmpty(), () -> "erros: " + resultado.erros());
        assertTrue(resultado.textoInalterado(),
                () -> "texto alterado: " + resultado.caracteresAntes() + " -> " + resultado.caracteresDepois());
        assertTrue(resultado.ficheirosGerados().size() >= 4, "" + resultado.ficheirosGerados());
        for (String nome : resultado.ficheirosGerados()) {
            HinarioNovo hinario = MAPPER.readValue(dataFiles.resolve("export/" + nome).toFile(), HinarioNovo.class);
            assertFalse(hinario.hinos().isEmpty(), nome);
            long linhas = hinario.hinos().stream()
                    .flatMap(h -> h.blocos().stream())
                    .flatMap(b -> b.linhas().stream())
                    .count();
            assertTrue(linhas > 0, nome);
        }
        assertTrue(Files.exists(resultado.relatorio()));
    }
}
