package com.ravunana.hinario.leitura;

import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rotação de modelos do Gemini (PRD 5.8): 429 forçado muda de modelo,
 * 429 diário pausa até ao reinício, todos pausados pausam a leitura e o
 * cache não gasta quota. Aceitação da Fase 3.
 */
class ModelPoolTest {

    @TempDir
    Path temp;

    private DataFiles dataFiles;
    private GeminiConfig config;
    private HttpFalso http;
    private EstadoQuota quota;
    private CacheLeitura cache;
    private ModelPool pool;
    private RelogioFalso relogio;

    @BeforeEach
    void preparar() throws IOException {
        dataFiles = new DataFiles(temp.toString());
        config = new GeminiConfig();
        config.setApiKey("chave-de-teste");
        config.setModelos(new ArrayList<>(List.of("modelo-a", "modelo-b")));
        config.setTentativasPorModelo(2);
        config.setEspera429MinutoSegundos(0);
        http = new HttpFalso();
        relogio = new RelogioFalso();
        quota = new EstadoQuota(dataFiles, config, relogio);
        cache = new CacheLeitura(dataFiles, config);
        GeminiPedido pedido = new GeminiPedido(config, http, new RespostaGeminiValidador());
        pool = new ModelPool(config, pedido, quota, cache, new ContextoPagina(dataFiles), dataFiles);
    }

    private Pagina importarPagina(int numero) throws IOException {
        // cor distinta por página: fotos iguais partilhariam o cache (PRD 5.8)
        BufferedImage imagem = new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(new Color(numero % 256, (numero * 7) % 256, 200));
            g.fillRect(0, 0, 60, 40);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        PaginaService servico = new PaginaService(dataFiles, new com.ravunana.hinario.pagina.ImagemProcessadora());
        return servico.importar(List.of(new PaginaService.PedidoImportacao(
                new MockMultipartFile("ficheiros", numero + ".jpg", "image/jpeg", saida.toByteArray()),
                numero)), "kmb", 0).get(0);
    }

    @Test
    void leNoPrimeiroModeloEGuardaCache() throws Exception {
        Pagina pagina = importarPagina(10);
        http.responder(RespostasGemini.okValida(10));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-a", resultado.modelo());
        assertEquals("gemini", resultado.fornecedor());
        assertEquals("ok", resultado.tentativas().get(0).resultado());
        assertEquals("api", resultado.tentativas().get(0).origem());
        assertEquals(1, http.modelos.size());
        assertNotNull(resultado.resposta().path("blocos"));
    }

    @Test
    void erro429PorMinutoRepeteNoMesmoModelo() throws Exception {
        Pagina pagina = importarPagina(11);
        http.responder(
                RespostasGemini.status(429, "Resource has been exhausted (e.g. check quota)."),
                RespostasGemini.okValida(11));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-a", resultado.modelo());
        assertEquals(List.of("modelo-a", "modelo-a"), http.modelos);
        assertEquals(List.of("429", "ok"),
                resultado.tentativas().stream().map(LeituraResultado.Tentativa::resultado).toList());
    }

    @Test
    void erro429PorMinutoEsgotadoPassaAoModeloSeguinte() throws Exception {
        Pagina pagina = importarPagina(12);
        String quotaMinuto = "{\"error\":{\"code\":429,\"message\":\"Quota exceeded for "
                + "quota metric 'Generate requests per minute'\"}}";
        http.responder(
                RespostasGemini.status(429, quotaMinuto),
                RespostasGemini.status(429, quotaMinuto),
                RespostasGemini.okValida(12));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-b", resultado.modelo());
        assertEquals(List.of("modelo-a", "modelo-a", "modelo-b"), http.modelos);
        // modelo-a fica em pausa curta depois de esgotar as tentativas
        assertNotNull(quota.pausadoAte("modelo-a"));
        assertNull(quota.pausadoAte("modelo-b"));
    }

    @Test
    void erro429DiarioPausaModeloAteAoReinicio() throws Exception {
        Pagina pagina = importarPagina(13);
        http.responder(
                RespostasGemini.status(429, "Quota exceeded for quota metric 'Generate requests per day'"),
                RespostasGemini.okValida(13));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-b", resultado.modelo());
        Instant pausadoAte = quota.pausadoAte("modelo-a");
        assertNotNull(pausadoAte);
        // reinício diário no fuso da quota (America/Los_Angeles)
        Instant esperado = Instant.parse("2026-10-29T07:00:00Z");
        assertEquals(esperado, pausadoAte);
        assertTrue(quota.pausado("modelo-a"));
    }

    @Test
    void todosOsModelosPausadosLancaQuotaEsgotada() throws Exception {
        Pagina pagina = importarPagina(14);
        quota.pausarAteReinicioDiario("modelo-a");
        quota.pausarAteReinicioDiario("modelo-b");

        assertThrows(QuotaEsgotadaExcecao.class, () -> pool.ler(pagina));
        assertEquals(0, http.modelos.size());
    }

    @Test
    void quotaDiariaEsgotadaNosDoisModelosPausaEQuandoPassaRetoma() throws Exception {
        Pagina pagina = importarPagina(15);
        http.responder(
                RespostasGemini.status(429, "Quota exceeded per day"),
                RespostasGemini.status(429, "Quota exceeded per day"));

        assertThrows(QuotaEsgotadaExcecao.class, () -> pool.ler(pagina));
        assertTrue(quota.pausado("modelo-a"));
        assertTrue(quota.pausado("modelo-b"));

        // passa o reinício diário → a leitura retoma sozinha (PRD 5.8.5)
        relogio.avancarSegundos(24 * 3600);
        http.responder(RespostasGemini.okValida(15));
        LeituraResultado resultado = pool.ler(pagina);
        assertEquals("modelo-a", resultado.modelo());
    }

    @Test
    void respostaInvalidaRepeteUmaVezNoMesmoModelo() throws Exception {
        Pagina pagina = importarPagina(16);
        http.responder(
                RespostasGemini.ok("isto não é JSON"),
                RespostasGemini.okValida(16));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-a", resultado.modelo());
        assertEquals(List.of("erro", "ok"),
                resultado.tentativas().stream().map(LeituraResultado.Tentativa::resultado).toList());
    }

    @Test
    void respostaInvalidaDuasVezesPassaAoSeguinte() throws Exception {
        Pagina pagina = importarPagina(17);
        http.responder(
                RespostasGemini.ok("não é JSON"),
                RespostasGemini.ok("também não é"),
                RespostasGemini.okValida(17));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-b", resultado.modelo());
        assertEquals(List.of("modelo-a", "modelo-a", "modelo-b"), http.modelos);
    }

    @Test
    void respostaBloqueadaPassaAoSeguinte() throws Exception {
        Pagina pagina = importarPagina(18);
        http.responder(RespostasGemini.bloqueada(), RespostasGemini.okValida(18));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-b", resultado.modelo());
        assertEquals("bloqueado", resultado.tentativas().get(0).resultado());
    }

    @Test
    void todosOsModelosBloqueadosLancaBloqueada() throws Exception {
        Pagina pagina = importarPagina(19);
        http.responder(RespostasGemini.bloqueada(), RespostasGemini.bloqueadaPorFeedback());

        assertThrows(BloqueadaExcecao.class, () -> pool.ler(pagina));
        assertEquals(List.of("modelo-a", "modelo-b"), http.modelos);
    }

    @Test
    void erroTemporarioRepeteComEsperaCrescente() throws Exception {
        Pagina pagina = importarPagina(20);
        http.responder(
                RespostasGemini.status(503, "service unavailable"),
                RespostasGemini.okValida(20));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-a", resultado.modelo());
        assertEquals(List.of("erro", "ok"),
                resultado.tentativas().stream().map(LeituraResultado.Tentativa::resultado).toList());
    }

    @Test
    void erroDefinitivoNaoSeRepeteEPassaAoSeguinte() throws Exception {
        Pagina pagina = importarPagina(21);
        http.responder(
                RespostasGemini.status(400, "invalid argument"),
                RespostasGemini.okValida(21));

        LeituraResultado resultado = pool.ler(pagina);

        assertEquals("modelo-b", resultado.modelo());
        assertEquals(2, http.modelos.size());
    }

    @Test
    void cacheNaoGastaQuotaNemChamaAApi() throws Exception {
        Pagina pagina = importarPagina(22);
        http.responder(RespostasGemini.okValida(22));
        pool.ler(pagina);
        assertEquals(1, http.modelos.size());

        LeituraResultado segunda = pool.ler(pagina);

        assertEquals(1, http.modelos.size(), "o cache deve evitar a segunda chamada");
        assertEquals("cache", segunda.tentativas().get(0).origem());
        assertEquals("ok", segunda.tentativas().get(0).resultado());
    }

    @Test
    void contaPedidosNoEstadoDaQuota() throws Exception {
        Pagina pagina = importarPagina(23);
        http.responder(RespostasGemini.okValida(23));
        pool.ler(pagina);
        pool.ler(pagina);

        EstadoQuota.ModeloQuota estado = quota.snapshot().get("modelo-a");
        assertEquals(1, estado.getPedidosDia(), "as chamadas ao cache não contam");
    }
}
