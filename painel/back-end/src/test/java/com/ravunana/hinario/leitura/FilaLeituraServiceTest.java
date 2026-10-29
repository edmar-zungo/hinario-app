package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.comparacao.AlfabetoVerificador;
import com.ravunana.hinario.comparacao.ComparacaoService;
import com.ravunana.hinario.comparacao.TesseractComando;
import com.ravunana.hinario.comparacao.TesseractConfig;
import com.ravunana.hinario.comparacao.TesseractProvider;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.ImagemProcessadora;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fila sequencial e estados (PRD Fase 3 e 4): `em_fila` → `lida` →
 * `comparada`; quota esgotada → `pausada_quota` **retoma sozinha**; falhas →
 * `erro_leitura` e `bloqueada_pela_ia` (que segue só com Tesseract).
 */
class FilaLeituraServiceTest {

    private static final String CABECALHO_TSV =
            "level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext\n";

    @TempDir
    Path temp;

    private DataFiles dataFiles;
    private GeminiConfig config;
    private TesseractConfig configTesseract;
    private HttpFalso http;
    private RelogioFalso relogio;
    private EstadoQuota quota;
    private PaginaService paginas;
    private FilaLeituraService fila;

    /** Saída do Tesseract falso; null = linhas do esquema 5.4 do teste. */
    private String tsvTesseract;
    private boolean falhaTesseract;

    @BeforeEach
    void preparar() {
        dataFiles = new DataFiles(temp.toString());
        config = new GeminiConfig();
        config.setApiKey("chave-de-teste");
        config.setModelos(new ArrayList<>(List.of("modelo-a", "modelo-b")));
        config.setTentativasPorModelo(2);
        config.setEspera429MinutoSegundos(0);
        configTesseract = new TesseractConfig();
        configTesseract.setEnabled(false); // a maioria dos testes é só Gemini
        http = new HttpFalso();
        relogio = new RelogioFalso();
        quota = new EstadoQuota(dataFiles, config, relogio);
        CacheLeitura cache = new CacheLeitura(dataFiles, config);
        GeminiPedido pedido = new GeminiPedido(config, http, new RespostaGeminiValidador());
        ModelPool pool = new ModelPool(config, pedido, quota, cache, new ContextoPagina(dataFiles), dataFiles);
        GeminiProvider gemini = new GeminiProvider(config, pool);
        TesseractComando comando = (imagem, cfg) -> {
            if (falhaTesseract) {
                throw new IOException("binário em falta");
            }
            return tsvTesseract == null ? CABECALHO_TSV : tsvTesseract;
        };
        TesseractProvider tesseract = new TesseractProvider(comando, configTesseract, dataFiles);
        ComparacaoService comparacao = new ComparacaoService(dataFiles);
        AlfabetoVerificador alfabeto = new AlfabetoVerificador(dataFiles);
        paginas = new PaginaService(dataFiles, new ImagemProcessadora());
        fila = new FilaLeituraService(gemini, config, quota, tesseract, comparacao, alfabeto,
                paginas, dataFiles, 100);
    }

    @AfterEach
    void parar() {
        fila.parar();
    }

    private Pagina importar(int numero) throws IOException {
        // cor distinta por página: fotos iguais partilhariam o cache (PRD 5.8)
        BufferedImage imagem = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(new Color(numero % 256, (numero * 7) % 256, 200));
            g.fillRect(0, 0, 40, 30);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        return paginas.importar(List.of(new PaginaService.PedidoImportacao(
                new MockMultipartFile("ficheiros", numero + ".jpg", "image/jpeg", saida.toByteArray()),
                numero)), "kmb", 0).get(0);
    }

    private Pagina esperarEstado(int numero, EstadoPagina esperado) throws InterruptedException, IOException {
        long limite = System.currentTimeMillis() + 8000;
        Pagina atual = null;
        while (System.currentTimeMillis() < limite) {
            atual = paginas.obter(numero);
            if (atual.estado() == esperado) {
                return atual;
            }
            Thread.sleep(50);
        }
        assertNotNull(atual);
        assertEquals(esperado, atual.estado(), "estado inesperado na página " + numero);
        return atual;
    }

    private static String tsv(String... linhas) {
        StringBuilder sb = new StringBuilder(CABECALHO_TSV);
        int numeroLinha = 1;
        for (String linha : linhas) {
            int numeroPalavra = 1;
            for (String palavra : linha.split(" ")) {
                sb.append("5\t1\t1\t1\t").append(numeroLinha).append('\t').append(numeroPalavra++)
                        .append("\t0\t0\t10\t10\t90\t").append(palavra).append('\n');
            }
            numeroLinha++;
        }
        return sb.toString();
    }

    @Test
    void lePaginaEEmficheiroLida() throws Exception {
        importar(1);
        http.responder(RespostasGemini.okValida(1));
        fila.arrancar();

        Pagina enfileirada = fila.enfileirar(1);
        assertEquals(EstadoPagina.EM_FILA, enfileirada.estado());

        Pagina lida = esperarEstado(1, EstadoPagina.LIDA);
        assertEquals("modelo-a", lida.leituras().path("gemini").path("modelo").asText());
        assertEquals("ok", lida.leituras().path("gemini").path("tentativas").get(0).path("resultado").asText());
        assertTrue(lida.leituras().path("gemini").path("resposta").path("blocos").isArray());
        assertTrue(lida.leituras().path("gemini").path("validacao").path("alfabeto").isArray());
    }

    @Test
    void comTesseractComparaEFicaComparada() throws Exception {
        configTesseract.setEnabled(true);
        tsvTesseract = tsv("M’ uhukunii aJesus.", "[?] kuanza");
        importar(10);
        http.responder(RespostasGemini.okValida(10));
        fila.arrancar();
        fila.enfileirar(10);

        Pagina comparada = esperarEstado(10, EstadoPagina.COMPARADA);
        JsonNode linhasTesseract = comparada.leituras().path("tesseract").path("linhas");
        assertEquals(2, linhasTesseract.size());
        assertEquals(0.9, linhasTesseract.get(0).path("confianca").asDouble());

        JsonNode divergencias = comparada.divergencias();
        assertEquals(1, divergencias.size());
        assertEquals("so_pontuacao", divergencias.get(0).path("tipo").asText());
        assertEquals("M’ uhukunii aJesus", divergencias.get(0).path("gemini").asText());
        assertEquals("M’ uhukunii aJesus.", divergencias.get(0).path("tesseract").asText());
        assertEquals(1, divergencias.get(0).path("bloco").asInt());
        assertEquals(0, divergencias.get(0).path("linha").asInt());
    }

    @Test
    void falhaDoTesseractNaoImpedeAGemini() throws Exception {
        configTesseract.setEnabled(true);
        falhaTesseract = true;
        importar(11);
        http.responder(RespostasGemini.okValida(11));
        fila.arrancar();
        fila.enfileirar(11);

        Pagina lida = esperarEstado(11, EstadoPagina.LIDA);
        assertTrue(lida.leituras().path("tesseract").path("erro").isTextual());
        assertTrue(lida.divergencias() == null || lida.divergencias().isEmpty());
        assertTrue(lida.leituras().path("gemini").path("resposta").path("blocos").isArray());
    }

    @Test
    void modoSoTesseractLemSemGemini() throws Exception {
        config.setEnabled(false);
        configTesseract.setEnabled(true);
        tsvTesseract = tsv("M’ uhukunii aJesus", "[?] kuanza");
        importar(12);
        fila.arrancar();

        // sem chave e com Gemini desactivado, o Tesseract chega para enfileirar
        fila.enfileirar(12);
        Pagina lida = esperarEstado(12, EstadoPagina.LIDA);
        assertEquals(2, lida.leituras().path("tesseract").path("linhas").size());
        assertTrue(lida.leituras().path("gemini").isMissingNode());
    }

    @Test
    void bloqueadaPelaIaSegueSoComTesseract() throws Exception {
        configTesseract.setEnabled(true);
        tsvTesseract = tsv("apenas tesseract");
        importar(13);
        http.responder(RespostasGemini.bloqueada(), RespostasGemini.bloqueada());
        fila.arrancar();
        fila.enfileirar(13);

        Pagina bloqueada = esperarEstado(13, EstadoPagina.BLOQUEADA_PELA_IA);
        assertEquals("apenas tesseract",
                bloqueada.leituras().path("tesseract").path("linhas").get(0).path("texto").asText());
    }

    @Test
    void priorizaPorNumeroDePagina() throws Exception {
        importar(30);
        importar(29);
        // as respostas vão pela ordem das chamadas: a primeira terá de ser a 29
        http.responder(RespostasGemini.okValida(29), RespostasGemini.okValida(30));

        fila.enfileirar(30);
        fila.enfileirar(29);
        fila.arrancar();

        Pagina vinteNove = esperarEstado(29, EstadoPagina.LIDA);
        Pagina trinta = esperarEstado(30, EstadoPagina.LIDA);
        assertEquals(29, vinteNove.leituras().path("gemini").path("resposta")
                .path("numeroPaginaImpresso").asInt());
        assertEquals(30, trinta.leituras().path("gemini").path("resposta")
                .path("numeroPaginaImpresso").asInt());
        assertEquals(2, http.chamadas());
    }

    @Test
    void quotaEsgotadaPausaPaginaERetomaSozinha() throws Exception {
        importar(2);
        http.responder(
                RespostasGemini.status(429, "Quota exceeded for quota metric 'Generate requests per day'"),
                RespostasGemini.status(429, "Quota exceeded for quota metric 'Generate requests per day'"));
        fila.arrancar();
        fila.enfileirar(2);

        Pagina pausada = esperarEstado(2, EstadoPagina.PAUSADA_QUOTA);
        assertEquals("429", pausada.leituras().path("gemini").path("tentativas").get(0).path("resultado").asText());
        assertTrue(fila.estado().quotaEsgotada());

        // prepara a resposta válida e passa o reinício diário → retoma sozinha
        http.responder(RespostasGemini.okValida(2));
        relogio.avancarSegundos(24 * 3600);

        Pagina lida = esperarEstado(2, EstadoPagina.LIDA);
        assertEquals("modelo-a", lida.leituras().path("gemini").path("modelo").asText());
        assertEquals(EstadoPagina.LIDA, paginas.obter(2).estado());
    }

    @Test
    void falhaDefinitivaFicaEmErroLeitura() throws Exception {
        importar(3);
        http.responder(
                RespostasGemini.status(400, "invalid argument"),
                RespostasGemini.status(400, "invalid argument"));
        fila.arrancar();
        fila.enfileirar(3);

        Pagina erro = esperarEstado(3, EstadoPagina.ERRO_LEITURA);
        assertEquals(2, erro.leituras().path("gemini").path("tentativas").size());
        assertTrue(erro.leituras().path("gemini").path("erro").isTextual());
    }

    @Test
    void todosBloqueadosFicaBloqueadaPelaIa() throws Exception {
        importar(4);
        http.responder(RespostasGemini.bloqueada(), RespostasGemini.bloqueada());
        fila.arrancar();
        fila.enfileirar(4);

        Pagina bloqueada = esperarEstado(4, EstadoPagina.BLOQUEADA_PELA_IA);
        assertEquals("bloqueado",
                bloqueada.leituras().path("gemini").path("tentativas").get(0).path("resultado").asText());
    }

    @Test
    void reencaminhaAPaginaQueFicouEmFilaNoArranque() throws Exception {
        Pagina pagina = importar(5);
        paginas.atualizar(pagina.comEstado(EstadoPagina.EM_FILA));
        http.responder(RespostasGemini.okValida(5));
        fila.arrancar();

        esperarEstado(5, EstadoPagina.LIDA);
    }

    @Test
    void estadoConfirmadoNaoPermiteReleitura() throws Exception {
        Pagina pagina = importar(6);
        paginas.atualizar(pagina.comEstado(EstadoPagina.CONFIRMADA));
        fila.arrancar();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> fila.enfileirar(6));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test
    void paginaInexistenteDá404() {
        fila.arrancar();
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> fila.enfileirar(999));
        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    @Test
    void semChaveDá409() throws Exception {
        importar(7);
        config.setApiKey("");
        fila.arrancar();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> fila.enfileirar(7));
        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertTrue(erro.getReason().contains("GEMINI_API_KEY"));
    }

    @Test
    void estadoDaFilaMostraPendentesEQuotas() throws Exception {
        importar(8);
        http.responder(RespostasGemini.okValida(8));
        fila.arrancar();
        fila.enfileirar(8);
        esperarEstado(8, EstadoPagina.LIDA);

        FilaEstado estado = fila.estado();
        assertEquals(0, estado.pendentes());
        assertEquals("ociosa", estado.estado());
        assertTrue(estado.geminiDisponivel());
        assertTrue(!estado.tesseractDisponivel());
        assertEquals(2, estado.modelos().size());
        assertEquals("modelo-a", estado.modelos().get(0).modelo());
    }
}
