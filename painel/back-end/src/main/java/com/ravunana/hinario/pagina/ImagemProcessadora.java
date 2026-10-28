package com.ravunana.hinario.pagina;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Pré-processamento mínimo da foto antes da leitura (PRD secção 3.1):
 * rodar, reduzir o lado maior para 2000 px e melhorar o contraste,
 * guardando o resultado em JPEG.
 *
 * Implementado com Java2D (JDK puro, sem dependências nativas) em vez de
 * JavaCV: mesma função, portátil e sem binários de terceiros.
 */
@Component
public class ImagemProcessadora {

    /** Lado maior máximo da imagem processada (PRD secção 5.2). */
    public static final int LADO_MAXIMO = 2000;

    /** Percentagem de cauda cortada em cada extremo no realce de contraste. */
    private static final int CAUDA_PERCENTAGEM = 2;

    public record Resultado(byte[] jpeg, int largura, int altura) {
    }

    /**
     * Converte {@code original} em JPEG processado.
     *
     * @param rotacaoGraus rotação em graus horários (0 = como está)
     * @throws IOException se os bytes não forem uma imagem legível
     */
    public Resultado processar(byte[] original, double rotacaoGraus) throws IOException {
        BufferedImage imagem = ImageIO.read(new ByteArrayInputStream(original));
        if (imagem == null) {
            throw new IOException("formato de imagem não suportado");
        }
        imagem = paraRgb(imagem);
        if (rotacaoGraus % 360 != 0) {
            imagem = rodar(imagem, rotacaoGraus);
        }
        imagem = reduzir(imagem, LADO_MAXIMO);
        imagem = realcarContraste(imagem);

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        if (!ImageIO.write(imagem, "jpg", saida)) {
            throw new IOException("não foi possível converter a imagem para JPEG");
        }
        return new Resultado(saida.toByteArray(), imagem.getWidth(), imagem.getHeight());
    }

    private static BufferedImage paraRgb(BufferedImage imagem) {
        if (imagem.getType() == BufferedImage.TYPE_INT_RGB) {
            return imagem;
        }
        BufferedImage rgb = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.drawImage(imagem, 0, 0, Color.WHITE, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static BufferedImage rodar(BufferedImage imagem, double graus) {
        double radianos = Math.toRadians(graus);
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        int novaLargura;
        int novaAltura;
        int voltas = 0;

        boolean ortogonal = graus % 90 == 0;
        if (ortogonal) {
            voltas = (int) Math.round(graus / 90) % 4;
            if (voltas < 0) {
                voltas += 4;
            }
            if (voltas == 0) {
                return imagem;
            }
            if (voltas % 2 == 1) {
                novaLargura = altura;
                novaAltura = largura;
            } else {
                novaLargura = largura;
                novaAltura = altura;
            }
        } else {
            double cosseno = Math.abs(Math.cos(radianos));
            double seno = Math.abs(Math.sin(radianos));
            novaLargura = (int) Math.ceil(largura * cosseno + altura * seno);
            novaAltura = (int) Math.ceil(largura * seno + altura * cosseno);
        }

        BufferedImage saida = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = saida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, novaLargura, novaAltura);
            g.rotate(radianos, novaLargura / 2.0, novaAltura / 2.0);
            g.drawImage(imagem, (novaLargura - largura) / 2, (novaAltura - altura) / 2, null);
        } finally {
            g.dispose();
        }
        return saida;
    }

    private static BufferedImage reduzir(BufferedImage imagem, int maximo) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        int maior = Math.max(largura, altura);
        if (maior <= maximo) {
            return imagem;
        }
        double fator = maximo / (double) maior;
        int novaLargura = Math.max(1, (int) Math.round(largura * fator));
        int novaAltura = Math.max(1, (int) Math.round(altura * fator));

        BufferedImage saida = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = saida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(imagem, 0, 0, novaLargura, novaAltura, null);
        } finally {
            g.dispose();
        }
        return saida;
    }

    /**
     * Estica o histograma cortando as caudas luminosas de 2% em cada extremo.
     * Quando a imagem já usa bem o intervalo, devolve-a intacta.
     */
    private static BufferedImage realcarContraste(BufferedImage imagem) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        int total = largura * altura;
        int[] pixeis = imagem.getRGB(0, 0, largura, altura, null, 0, largura);

        int[] histograma = new int[256];
        for (int pixel : pixeis) {
            int vermelho = (pixel >> 16) & 0xFF;
            int verde = (pixel >> 8) & 0xFF;
            int azul = pixel & 0xFF;
            int luminancia = (int) (0.299 * vermelho + 0.587 * verde + 0.114 * azul);
            histograma[Math.min(255, Math.max(0, luminancia))]++;
        }

        int cauda = Math.max(1, total * CAUDA_PERCENTAGEM / 100);
        int inferior = 0;
        int superior = 255;
        long acumulado = 0;
        for (int i = 0; i < 256; i++) {
            acumulado += histograma[i];
            if (acumulado > cauda) {
                inferior = i;
                break;
            }
        }
        acumulado = 0;
        for (int i = 255; i >= 0; i--) {
            acumulado += histograma[i];
            if (acumulado > cauda) {
                superior = i;
                break;
            }
        }
        if (superior - inferior < 16) {
            return imagem;
        }

        byte[] lut = new byte[256];
        for (int i = 0; i < 256; i++) {
            int valor = Math.round((i - inferior) * 255f / (superior - inferior));
            lut[i] = (byte) Math.min(255, Math.max(0, valor));
        }
        return new LookupOp(new ByteLookupTable(0, lut), null).filter(imagem, null);
    }
}
