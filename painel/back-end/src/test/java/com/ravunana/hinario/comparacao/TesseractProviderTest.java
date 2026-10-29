package com.ravunana.hinario.comparacao;

import com.ravunana.hinario.leitura.LeituraExcecao;
import com.ravunana.hinario.leitura.LeituraResultado;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.services.DataFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TSV do Tesseract → linhas com confiança (PRD 3.2 {@code leituras.tesseract}). */
class TesseractProviderTest {

    @TempDir
    Path temp;

    private static String tsvLinha(int linha, String... palavras) {
        StringBuilder sb = new StringBuilder();
        int palavra = 1;
        for (String p : palavras) {
            sb.append("5\t1\t1\t1\t").append(linha).append("\t").append(palavra++)
                    .append("\t0\t0\t10\t10\t90\t").append(p).append("\n");
        }
        return sb.toString();
    }

    private static final String CABECALHO =
            "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n";

    private TesseractConfig config() {
        TesseractConfig config = new TesseractConfig();
        config.setIdioma("por");
        return config;
    }

    @Test
    void parseTsvAgrupaPalavrasPorLinha() {
        String tsv = CABECALHO
                + tsvLinha(1, "M’", "uhukunii")
                + tsvLinha(2, "[?]");
        List<TesseractProvider.LinhaLida> linhas = TesseractProvider.parseTsv(tsv);
        assertEquals(2, linhas.size());
        assertEquals("M’ uhukunii", linhas.get(0).texto());
        assertEquals(0.9, linhas.get(0).confianca());
        assertEquals("[?]", linhas.get(1).texto());
    }

    @Test
    void parseTsvIgnoraLinhasDeNivelEDuplicadosDeEstrutura() {
        String tsv = CABECALHO
                + "4\t1\t1\t1\t0\t0\t0\t0\t10\t10\t-1\t\n" // nível < 5: descartar
                + "5\t1\t1\t1\t1\t1\t0\t0\t10\t10\t-1\t\n" // conf negativa: descartar
                + tsvLinha(1, "kuanza");
        List<TesseractProvider.LinhaLida> linhas = TesseractProvider.parseTsv(tsv);
        assertEquals(1, linhas.size());
        assertEquals("kuanza", linhas.get(0).texto());
    }

    @Test
    void confiancaDaLinhaÉAMédiaDasPalavras() {
        String tsv = CABECALHO
                + "5\t1\t1\t1\t1\t1\t0\t0\t10\t10\t80\tM’\n"
                + "5\t1\t1\t1\t1\t2\t0\t0\t10\t10\t100\tukia\n";
        List<TesseractProvider.LinhaLida> linhas = TesseractProvider.parseTsv(tsv);
        assertEquals(0.9, linhas.get(0).confianca());
    }

    @Test
    void lerUsaAImagemProcessadaEPreencheLinhas() throws Exception {
        Path processada = temp.resolve("imagens/processada/0007.jpg");
        Files.createDirectories(processada.getParent());
        Files.write(processada, new byte[]{1, 2, 3});
        AtomicReference<Path> usado = new AtomicReference<>();
        TesseractComando comando = (imagem, cfg) -> {
            usado.set(imagem);
            return CABECALHO + tsvLinha(1, "M’", "uhukunii");
        };
        TesseractProvider provider = new TesseractProvider(comando, config(), new DataFiles(temp.toString()));

        Pagina pagina = new Pagina(7, "kmb", "imagens/original/0007.jpg", "imagens/processada/0007.jpg",
                "sha256:x", EstadoPagina.LIDA, "0007.jpg", "2026-10-28T10:00:00Z",
                100, 80, null, null, null);
        LeituraResultado resultado = provider.ler(pagina);

        assertEquals(processada, usado.get());
        assertEquals("tesseract", resultado.fornecedor());
        assertEquals(1, resultado.resposta().get("linhas").size());
        assertEquals("M’ uhukunii", resultado.resposta().get("linhas").get(0).get("texto").asText());
        assertEquals(0.9, resultado.resposta().get("linhas").get(0).get("confianca").asDouble());
    }

    @Test
    void falhaDoBinarioViraLeituraExcecao() throws Exception {
        Path processada = temp.resolve("imagens/processada/0001.jpg");
        Files.createDirectories(processada.getParent());
        Files.write(processada, new byte[]{1});
        TesseractComando comando = (imagem, cfg) -> {
            throw new IOException("não encontrado");
        };
        TesseractProvider provider = new TesseractProvider(comando, config(), new DataFiles(temp.toString()));
        Pagina pagina = new Pagina(1, "kmb", "imagens/original/0001.jpg", "imagens/processada/0001.jpg",
                "sha256:x", EstadoPagina.LIDA, "0001.jpg", "2026-10-28T10:00:00Z",
                100, 80, null, null, null);

        LeituraExcecao erro = assertThrows(LeituraExcecao.class, () -> provider.ler(pagina));
        assertTrue(erro.getMessage().contains("não encontrado"));
    }

    @Test
    void indisponivelSemBinarioNemIdioma() {
        TesseractConfig config = config();
        assertTrue(config.disponivel());
        config.setEnabled(false);
        assertTrue(!config.disponivel());
    }
}
