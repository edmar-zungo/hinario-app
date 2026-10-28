package com.ravunana.hinario.leitura;

import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * `POST /paginas/{n}/ler` e `GET /fila` de ponta a ponta (PRD secção 7) com
 * um servidor simulado da API do Gemini: a foto entra, a fila lê e a página
 * fica `lida`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LeituraControllerTest {

    static final Path DADOS;
    static final HttpServer SERVIDOR;
    static final String URL_BASE;

    static {
        try {
            DADOS = Files.createTempDirectory("leitura-controller-test");
            SERVIDOR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVIDOR.createContext("/v1beta/models/", exchange -> {
                byte[] corpo = RespostasGemini.okValida(50).corpo()
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, corpo.length);
                try (OutputStream saida = exchange.getResponseBody()) {
                    saida.write(corpo);
                }
            });
            SERVIDOR.start();
            URL_BASE = "http://127.0.0.1:" + SERVIDOR.getAddress().getPort();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("hinario.dados.dir", DADOS::toString);
        registry.add("gemini.api-key", () -> "chave-de-teste");
        registry.add("gemini.url-base", () -> URL_BASE);
        registry.add("gemini.modelos", () -> "modelo-teste");
        registry.add("gemini.espera-429-minuto-segundos", () -> "0");
        registry.add("hinario.fila.retoma-ms", () -> "100");
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    PaginaService paginas;

    @Autowired
    FilaLeituraService fila;

    @BeforeEach
    void garantirFila() {
        fila.arrancar();
    }

    private static byte[] foto() throws IOException {
        BufferedImage imagem = new BufferedImage(50, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(new Color(30, 210, 200));
            g.fillRect(0, 0, 50, 40);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        return saida.toByteArray();
    }

    private int importarPagina(int numero) throws Exception {
        paginas.importar(List.of(new PaginaService.PedidoImportacao(
                new MockMultipartFile("ficheiros", numero + ".jpg", "image/jpeg", foto()),
                numero)), "kmb", 0);
        return numero;
    }

    private Pagina esperarLida(int numero) throws Exception {
        long limite = System.currentTimeMillis() + 8000;
        Pagina atual = null;
        while (System.currentTimeMillis() < limite) {
            atual = paginas.obter(numero);
            if (atual.estado() == com.ravunana.hinario.pagina.EstadoPagina.LIDA) {
                return atual;
            }
            Thread.sleep(100);
        }
        assertEquals(com.ravunana.hinario.pagina.EstadoPagina.LIDA, atual.estado());
        return atual;
    }

    @Test
    void postLerDevolve202EPaginaFicaLida() throws Exception {
        int numero = importarPagina(50);

        mvc.perform(multipart("/paginas/" + numero + "/ler"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.pagina").value(50))
                .andExpect(jsonPath("$.estado").value("em_fila"));

        Pagina lida = esperarLida(numero);
        assertEquals("modelo-teste", lida.leituras().path("gemini").path("modelo").asText());
        assertTrue(lida.leituras().path("gemini").path("resposta").path("blocos").isArray());
        assertEquals("api", lida.leituras().path("gemini").path("tentativas")
                .get(0).path("origem").asText());
    }

    @Test
    void getFilaMostraEstadoEQuotasPorModelo() throws Exception {
        mvc.perform(get("/fila"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").exists())
                .andExpect(jsonPath("$.geminiDisponivel").value(true))
                .andExpect(jsonPath("$.modelos[0].modelo").value("modelo-teste"))
                .andExpect(jsonPath("$.modelos[0].pedidosDia").exists())
                .andExpect(jsonPath("$.pendentes").exists());
    }

    @Test
    void lerPaginaInexistenteDá404() throws Exception {
        mvc.perform(multipart("/paginas/999/ler"))
                .andExpect(status().isNotFound());
    }
}
