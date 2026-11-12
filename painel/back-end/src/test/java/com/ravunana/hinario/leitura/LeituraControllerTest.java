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
 * um servidor simulado da API Groq: a foto entra, a fila lê e estrutura
 * sem depender de um motor OCR local.
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
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    PaginaService paginas;

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

    @Test
    void postLerDevolve200EPaginaFicaEstruturada() throws Exception {
        int numero = importarPagina(50);

        mvc.perform(multipart("/paginas/" + numero + "/ler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(50))
                .andExpect(jsonPath("$.estado").value("estruturada"));

        Pagina lida = paginas.obter(numero);
        assertEquals("modelo-teste", lida.leituras().path("gemini").path("modelo").asText());
        assertTrue(lida.leituras().path("gemini").path("resposta").path("blocos").isArray());
        assertEquals("api", lida.leituras().path("gemini").path("tentativas")
                .get(0).path("origem").asText());
    }

    @Test
    void lerPaginaInexistenteDá404() throws Exception {
        mvc.perform(multipart("/paginas/999/ler"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postEstruturarMontaBlocosDaPaginaComparada() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode resposta = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ObjectNode bloco = resposta.putArray("blocos").addObject();
        bloco.put("tipo", "estrofe");
        bloco.putNull("numeroHino");
        bloco.putNull("titulo");
        bloco.put("numeroEstrofe", 1);
        bloco.put("italico", false);
        bloco.put("continuacaoDeBlocoAnterior", false);
        bloco.putArray("linhas").addObject().put("texto", "Ngana Jezu, nga mu zola,").put("recuo", 0);
        bloco.putArray("duvidas");
        com.fasterxml.jackson.databind.node.ObjectNode leituras = mapper.createObjectNode();
        leituras.putObject("gemini").set("resposta", resposta);
        paginas.atualizar(new Pagina(60, "kmb", "imagens/original/0060.jpg",
                "imagens/processada/0060.jpg", "sha256:60",
                com.ravunana.hinario.pagina.EstadoPagina.COMPARADA, "0060.jpg",
                "2026-10-28T10:00:00Z", 100, 80, leituras, null, null));

        mvc.perform(multipart("/paginas/60/estruturar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("estruturada"))
                .andExpect(jsonPath("$.blocos[0].tipo").value("estrofe"))
                .andExpect(jsonPath("$.blocos[0].linhas[0].texto").value("Ngana Jezu, nga mu zola,"));
    }

    @Test
    void postEstruturarPaginaInexistenteDá404() throws Exception {
        mvc.perform(multipart("/paginas/999/estruturar"))
                .andExpect(status().isNotFound());
    }
}
