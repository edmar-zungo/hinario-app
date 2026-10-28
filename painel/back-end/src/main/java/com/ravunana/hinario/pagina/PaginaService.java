package com.ravunana.hinario.pagina;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Importação de fotos de páginas (PRD secção 3.1 e Fase 2).
 *
 * Guarda o original por omissão, a versão processada e o ficheiro de
 * trabalho {@code paginas/<n>.json} com estado {@code importada}.
 * Reimportar a mesma página substitui tudo e repõe o estado.
 */
@Service
public class PaginaService {

    private static final List<String> EXTENSOES = List.of("jpg", "png", "webp", "bmp");

    private final DataFiles dataFiles;
    private final ImagemProcessadora processadora;
    private final ObjectMapper mapper;

    public PaginaService(DataFiles dataFiles, ImagemProcessadora processadora) {
        this.dataFiles = dataFiles;
        this.processadora = processadora;
        this.mapper = dataFiles.mapper();
    }

    /** Um ficheiro e o número de página a que pertence. */
    public record PedidoImportacao(MultipartFile ficheiro, int pagina) {
    }

    /**
     * Importa os pedidos (já emparelhados com o número de página), sempre
     * processados por ordem crescente de página.
     */
    public List<Pagina> importar(List<PedidoImportacao> pedidos, String hinarioId, double rotacao) {
        Linguas lingua;
        try {
            lingua = Linguas.porCodigo(hinarioId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (pedidos == null || pedidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nenhum ficheiro submetido");
        }

        List<PedidoImportacao> ordenados = new ArrayList<>(pedidos);
        for (PedidoImportacao pedido : ordenados) {
            if (pedido.ficheiro() == null || pedido.ficheiro().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ficheiro vazio");
            }
            if (pedido.pagina() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "número de página inválido: " + pedido.pagina());
            }
        }
        long distintos = ordenados.stream().map(PedidoImportacao::pagina).distinct().count();
        if (distintos != ordenados.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "número de página repetido no lote");
        }
        ordenados.sort(Comparator.comparingInt(PedidoImportacao::pagina));

        List<Pagina> importadas = new ArrayList<>();
        // duas fases: processar tudo em memória e só depois gravar, para que um
        // ficheiro inválido não deixe o lote pela metade
        List<Preparada> preparadas = new ArrayList<>();
        for (PedidoImportacao pedido : ordenados) {
            try {
                preparadas.add(preparar(pedido, lingua, rotacao));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "página " + pedido.pagina() + ": " + e.getMessage());
            }
        }
        for (int i = 0; i < preparadas.size(); i++) {
            try {
                gravar(preparadas.get(i));
                importadas.add(preparadas.get(i).pagina());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "página " + ordenados.get(i).pagina() + ": " + e.getMessage());
            }
        }
        return importadas;
    }

    /** Lista todas as páginas importadas, por ordem de número. */
    public List<Pagina> listar() throws IOException {
        Path pasta = dataFiles.subpasta("paginas");
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        List<Pagina> paginas = new ArrayList<>();
        try (Stream<Path> ficheiros = Files.list(pasta)) {
            List<Path> ordenados = ficheiros
                    .filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
            for (Path ficheiro : ordenados) {
                paginas.add(mapper.readValue(ficheiro.toFile(), Pagina.class));
            }
        }
        return paginas;
    }

    /** Lê o ficheiro de trabalho de uma página (404 se não existir). */
    public Pagina obter(int numero) throws IOException {
        Path ficheiro = caminhoPagina(numero);
        if (!Files.exists(ficheiro)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "página " + numero + " não importada");
        }
        return mapper.readValue(ficheiro.toFile(), Pagina.class);
    }

    /** Grava a página actualizada (estado, leituras, ...) em `paginas/<n>.json`. */
    public void atualizar(Pagina pagina) throws IOException {
        dataFiles.escreverJson(caminhoPagina(pagina.pagina()), pagina);
    }

    /** Página já processada, ainda por gravar. */
    private record Preparada(Pagina pagina, byte[] original, byte[] processada) {
    }

    private Preparada preparar(PedidoImportacao pedido, Linguas lingua, double rotacao) throws IOException {
        byte[] original = pedido.ficheiro().getBytes();
        String extensao = extensao(original, pedido.ficheiro().getOriginalFilename());
        ImagemProcessadora.Resultado processada = processadora.processar(original, rotacao);

        String nome = String.format("%04d.%s", pedido.pagina(), extensao);
        String nomeProcessada = String.format("%04d.jpg", pedido.pagina());
        Pagina pagina = new Pagina(
                pedido.pagina(),
                lingua.getCodigo(),
                "imagens/original/" + nome,
                "imagens/processada/" + nomeProcessada,
                "sha256:" + resumo(original),
                EstadoPagina.IMPORTADA,
                pedido.ficheiro().getOriginalFilename(),
                Instant.now().toString(),
                processada.largura(),
                processada.altura(),
                mapper.createObjectNode(),
                mapper.createArrayNode(),
                mapper.createArrayNode());
        return new Preparada(pagina, original, processada.jpeg());
    }

    private void gravar(Preparada preparada) throws IOException {
        Pagina pagina = preparada.pagina();
        dataFiles.escreverBytes(dataFiles.resolve(pagina.imagem()), preparada.original());
        dataFiles.escreverBytes(dataFiles.resolve(pagina.imagemProcessada()), preparada.processada());
        dataFiles.escreverJson(caminhoPagina(pagina.pagina()), pagina);
    }

    private Path caminhoPagina(int numero) {
        return dataFiles.subpasta("paginas").resolve(String.format("%04d.json", numero));
    }

    private static String resumo(byte[] conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /**
     * Extensão normalizada do ficheiro guardado. Usa o nome submetido quando
     * for um formato conhecido; caso contrário, pergunta ao leitor de imagens.
     */
    private static String extensao(byte[] bytes, String nomeOriginal) throws IOException {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            String candidata = nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT);
            if ("jpeg".equals(candidata)) {
                return "jpg";
            }
            if (EXTENSOES.contains(candidata)) {
                return candidata;
            }
        }
        try (ImageInputStream fluxo = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> leitores = fluxo == null ? null : ImageIO.getImageReaders(fluxo);
            if (leitores != null && leitores.hasNext()) {
                String formato = leitores.next().getFormatName().toLowerCase(Locale.ROOT);
                return "jpeg".equals(formato) ? "jpg" : formato;
            }
        }
        throw new IOException("formato de imagem não suportado");
    }
}
