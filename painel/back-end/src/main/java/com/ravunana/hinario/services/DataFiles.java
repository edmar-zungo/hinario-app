package com.ravunana.hinario.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Raiz da pasta de dados local (PRD secção 3.1) e escrita atómica de ficheiros.
 *
 * A raiz vem da propriedade {@code hinario.dados.dir} (ou da variável de
 * ambiente {@code HINARIO_DADOS_DIR}). Quando vazia, mantém o comportamento
 * histórico: pasta {@code data/} junto do back-end.
 */
@Component
public class DataFiles {

    private final Path raiz;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public DataFiles(@Value("${hinario.dados.dir:}") String configurado) {
        this.raiz = resolverRaiz(configurado);
    }

    public Path raiz() {
        return raiz;
    }

    public Path resolve(String nome) {
        return raiz.resolve(nome);
    }

    public Path subpasta(String nome) {
        return raiz.resolve(nome);
    }

    /** Garante a pasta (e as pai) e devolve o caminho. */
    public Path criarPasta(String nome) throws IOException {
        Path pasta = raiz.resolve(nome);
        Files.createDirectories(pasta);
        return pasta;
    }

    /** Escreve JSON de forma atómica (ficheiro temporário + move). */
    public void escreverJson(Path alvo, Object valor) throws IOException {
        Path pai = alvo.getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }
        Path temporario = Files.createTempFile(pai, alvo.getFileName().toString(), ".tmp");
        try {
            mapper.writeValue(temporario.toFile(), valor);
            mover(temporario, alvo);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    /** Escreve texto de forma atómica (UTF-8). */
    public void escreverTexto(Path alvo, String texto) throws IOException {
        Path pai = alvo.getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }
        Path temporario = Files.createTempFile(pai, alvo.getFileName().toString(), ".tmp");
        try {
            Files.write(temporario, texto.getBytes(StandardCharsets.UTF_8));
            mover(temporario, alvo);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    /** Escreve bytes de forma atómica (ficheiros binários, ex.: fotos). */
    public void escreverBytes(Path alvo, byte[] conteudo) throws IOException {
        Path pai = alvo.getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }
        Path temporario = Files.createTempFile(pai, alvo.getFileName().toString(), ".tmp");
        try {
            Files.write(temporario, conteudo);
            mover(temporario, alvo);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    private static void mover(Path origem, Path alvo) throws IOException {
        try {
            Files.move(origem, alvo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(origem, alvo, StandardCopyOption.REPLACE_EXISTING);
        }
        definirPermissoes(alvo);
    }

    /**
     * {@code Files.createTempFile} cria com 600; os ficheiros de dados devem
     * ficar legíveis pelo utilizador normal (644).
     */
    private static void definirPermissoes(Path ficheiro) {
        try {
            if (Files.getPosixFilePermissions(ficheiro) != null) {
                Files.setPosixFilePermissions(ficheiro, EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
            }
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            // sistema de ficheiros sem POSIX: manter as permissões por omissão
        }
    }

    private static Path resolverRaiz(String configurado) {
        if (configurado != null && !configurado.isBlank()) {
            String caminho = configurado.trim();
            if (caminho.startsWith("~/")) {
                caminho = System.getProperty("user.home") + caminho.substring(1);
            }
            return Path.of(caminho).toAbsolutePath().normalize();
        }
        return pastaDadosLegada().toPath().toAbsolutePath().normalize();
    }

    /** Procura a pasta data/ do back-end a partir do directório de trabalho. */
    private static File pastaDadosLegada() {
        File dir = new File(System.getProperty("user.dir"));
        for (File d = dir; d != null; d = d.getParentFile()) {
            if (new File(d, "pom.xml").exists() && new File(d, "mvnw").exists()) {
                return new File(d, "data");
            }
            File backend = new File(d, "painel/back-end");
            if (new File(backend, "pom.xml").exists()) {
                return new File(backend, "data");
            }
        }
        return new File(dir, "data");
    }
}
