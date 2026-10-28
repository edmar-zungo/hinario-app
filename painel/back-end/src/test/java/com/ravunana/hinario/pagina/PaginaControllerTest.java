package com.ravunana.hinario.pagina;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaginaControllerTest {

    static final Path DADOS;
    static {
        try {
            DADOS = Files.createTempDirectory("paginas-controller-test");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void pastaDeDados(DynamicPropertyRegistry registry) {
        registry.add("hinario.dados.dir", DADOS::toString);
    }

    @Autowired
    MockMvc mvc;

    private static byte[] imagem(int largura, int altura) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, largura, altura);
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, largura / 2, altura / 2);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        return saida.toByteArray();
    }

    @Test
    void importaFotoComNumeroExplicito() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("ficheiros", "foto-telemovel.jpg", "image/jpeg", imagem(80, 60));

        mvc.perform(multipart("/paginas").file(foto)
                        .param("pagina", "395")
                        .param("hinarioId", "kmb"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].pagina").value(395))
                .andExpect(jsonPath("$[0].hinarioId").value("kmb"))
                .andExpect(jsonPath("$[0].estado").value("importada"));
    }

    @Test
    void importaLoteOrdenadoEPorNomeDoFicheiro() throws Exception {
        byte[] conteudo = imagem(40, 30);
        MockMultipartFile um = new MockMultipartFile("ficheiros", "0396.jpg", "image/jpeg", conteudo);
        MockMultipartFile dois = new MockMultipartFile("ficheiros", "0395.jpg", "image/jpeg", conteudo);

        mvc.perform(multipart("/paginas").file(um).file(dois).param("hinarioId", "por"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pagina").value(395))
                .andExpect(jsonPath("$[1].pagina").value(396));
    }

    @Test
    void rejeitaSemHinario() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("ficheiros", "a.jpg", "image/jpeg", imagem(10, 10));

        mvc.perform(multipart("/paginas").file(foto).param("pagina", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaFicheiroQueNaoEImagem() throws Exception {
        MockMultipartFile lixo = new MockMultipartFile("ficheiros", "a.jpg", "image/jpeg",
                "não sou imagem".getBytes());

        mvc.perform(multipart("/paginas").file(lixo).param("pagina", "2").param("hinarioId", "kmb"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaPaginasEDevolve404QuandoNaoExiste() throws Exception {
        mvc.perform(get("/paginas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mvc.perform(get("/paginas/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aceitaImagemPngEGravaComoPng() throws Exception {
        MockMultipartFile png = new MockMultipartFile("ficheiros", "pagina.png", MediaType.IMAGE_PNG_VALUE,
                png(30, 20));

        mvc.perform(multipart("/paginas").file(png).param("pagina", "5").param("hinarioId", "kko"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].imagem").value("imagens/original/0005.png"))
                .andExpect(jsonPath("$[0].imagemProcessada").value("imagens/processada/0005.jpg"));
    }

    private static byte[] png(int largura, int altura) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(Color.YELLOW);
            g.fillRect(0, 0, largura, altura);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "png", saida);
        return saida.toByteArray();
    }
}
