package com.ravunana.hinario.pagina;

import com.ravunana.hinario.services.DataFiles;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginaServiceTest {

    @TempDir
    Path temp;

    private PaginaService servico() {
        return new PaginaService(new DataFiles(temp.toString()), new ImagemProcessadora());
    }

    private static byte[] imagem(int largura, int altura) throws IOException {
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, largura, altura);
            g.setColor(Color.BLACK);
            g.fillOval(5, 5, Math.max(1, largura / 3), Math.max(1, altura / 3));
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, "jpg", saida);
        return saida.toByteArray();
    }

    private static MockMultipartFile foto(String nome, byte[] conteudo) {
        return new MockMultipartFile("ficheiros", nome, "image/jpeg", conteudo);
    }

    @Test
    void importaFotoGuardandoOriginalProcessadaEStaging() throws IOException {
        byte[] conteudo = imagem(120, 80);
        Pagina pagina = servico().importar(
                List.of(new PaginaService.PedidoImportacao(foto("foto.jpg", conteudo), 395)), "kmb", 0).get(0);

        assertEquals(395, pagina.pagina());
        assertEquals("kmb", pagina.hinarioId());
        assertEquals(EstadoPagina.IMPORTADA, pagina.estado());
        assertEquals("imagens/original/0395.jpg", pagina.imagem());
        assertEquals("imagens/processada/0395.jpg", pagina.imagemProcessada());
        assertTrue(pagina.hash().startsWith("sha256:"));
        assertEquals(64, pagina.hash().substring("sha256:".length()).length());

        assertArrayEquals(conteudo, Files.readAllBytes(temp.resolve("imagens/original/0395.jpg")));
        assertTrue(Files.exists(temp.resolve("imagens/processada/0395.jpg")));
        assertTrue(Files.exists(temp.resolve("paginas/0395.json")));
    }

    @Test
    void loteEProcessadoPorOrdemDePagina() throws IOException {
        byte[] conteudo = imagem(60, 40);
        List<Pagina> importadas = servico().importar(List.of(
                new PaginaService.PedidoImportacao(foto("b.jpg", conteudo), 400),
                new PaginaService.PedidoImportacao(foto("a.jpg", conteudo), 395),
                new PaginaService.PedidoImportacao(foto("c.jpg", conteudo), 398)), "por", 0);

        assertEquals(List.of(395, 398, 400), importadas.stream().map(Pagina::pagina).toList());
        assertTrue(Files.exists(temp.resolve("paginas/0400.json")));
    }

    @Test
    void reimportarSobrescreveERepoeEstado() throws IOException {
        PaginaService servico = servico();
        servico.importar(List.of(new PaginaService.PedidoImportacao(foto("a.jpg", imagem(100, 50)), 10)), "kkr", 0);

        byte[] novo = imagem(200, 100);
        Pagina segunda = servico.importar(
                List.of(new PaginaService.PedidoImportacao(foto("b.jpg", novo), 10)), "kkr", 0).get(0);

        assertEquals(EstadoPagina.IMPORTADA, segunda.estado());
        assertEquals(200, segunda.largura());
        assertEquals(100, segunda.altura());
        assertEquals(1, servico.listar().size());
    }

    @Test
    void reduzLadoMaiorPara2000() throws IOException {
        Pagina pagina = servico().importar(
                List.of(new PaginaService.PedidoImportacao(foto("grande.jpg", imagem(2600, 800)), 20)), "kmb", 0).get(0);

        assertEquals(2000, pagina.largura());
        assertEquals(Math.round(800 * 2000f / 2600), pagina.altura());
    }

    @Test
    void roda90Graus() throws IOException {
        Pagina pagina = servico().importar(
                List.of(new PaginaService.PedidoImportacao(foto("rodada.jpg", imagem(100, 50)), 30)), "kmb", 90).get(0);

        assertEquals(50, pagina.largura());
        assertEquals(100, pagina.altura());
    }

    @Test
    void rejeitaBytesQueNaoSaoImagem() {
        MockMultipartFile lixo = foto("lixo.jpg", "isto não é imagem".getBytes());
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                servico().importar(List.of(new PaginaService.PedidoImportacao(lixo, 40)), "kmb", 0));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void naoGravaNadaQuandoAImagemEInvalida() throws IOException {
        MockMultipartFile lixo = foto("lixo.jpg", "isto não é imagem".getBytes());
        assertThrows(ResponseStatusException.class, () ->
                servico().importar(List.of(new PaginaService.PedidoImportacao(lixo, 41)), "kmb", 0));

        assertFalse(Files.exists(temp.resolve("imagens/original/0041.jpg")));
        assertFalse(Files.exists(temp.resolve("paginas/0041.json")));
    }

    @Test
    void loteInvalidoNaoDeixaNenhumaPaginaGravada() throws IOException {
        MockMultipartFile boa = foto("boa.jpg", imagem(20, 20));
        MockMultipartFile lixo = foto("lixo.jpg", "lixo".getBytes());
        assertThrows(ResponseStatusException.class, () ->
                servico().importar(List.of(
                        new PaginaService.PedidoImportacao(boa, 51),
                        new PaginaService.PedidoImportacao(lixo, 52)), "kmb", 0));

        assertFalse(Files.exists(temp.resolve("paginas/0051.json")));
        assertFalse(Files.exists(temp.resolve("paginas/0052.json")));
        assertEquals(List.of(), servico().listar());
    }

    @Test
    void rejeitaLinguaDesconhecida() throws IOException {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                servico().importar(List.of(new PaginaService.PedidoImportacao(foto("a.jpg", imagem(10, 10)), 50)),
                        "xxx", 0));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void rejeitaNumerosRepetidosNoLote() throws IOException {
        byte[] conteudo = imagem(10, 10);
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                servico().importar(List.of(
                        new PaginaService.PedidoImportacao(foto("a.jpg", conteudo), 60),
                        new PaginaService.PedidoImportacao(foto("b.jpg", conteudo), 60)), "kmb", 0));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void listaEObtemPaginasImportadas() throws IOException {
        PaginaService servico = servico();
        servico.importar(List.of(new PaginaService.PedidoImportacao(foto("a.jpg", imagem(20, 20)), 70)), "kmb", 0);

        assertEquals(1, servico.listar().size());
        assertEquals(70, servico.obter(70).pagina());
        assertThrows(ResponseStatusException.class, () -> servico.obter(999));
        assertEquals(List.of(), new PaginaService(new DataFiles(temp.resolve("vazio").toString()),
                new ImagemProcessadora()).listar());
    }
}
