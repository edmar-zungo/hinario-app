package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache de respostas da IA (PRD 5.8): chave = hash da imagem + versão do
 * prompt + modelo, em {@code cache/<hash>.json}. Reprocessar a mesma imagem
 * não gasta quota.
 */
@Component
public class CacheLeitura {

    private final DataFiles dataFiles;
    private final GeminiConfig config;

    public CacheLeitura(DataFiles dataFiles, GeminiConfig config) {
        this.dataFiles = dataFiles;
        this.config = config;
    }

    /** Chave do cache (PRD 5.8). */
    public String chave(String hashImagem, String modelo) {
        return resumoSha256(hashImagem + ":" + config.getVersaoPrompt() + ":" + modelo);
    }

    public boolean contem(String chave) {
        return Files.exists(caminho(chave));
    }

    /** Resposta em cache, ou null. */
    public JsonNode resposta(String chave) {
        Path ficheiro = caminho(chave);
        if (!Files.exists(ficheiro)) {
            return null;
        }
        try {
            JsonNode registo = dataFiles.mapper().readTree(ficheiro.toFile());
            JsonNode resposta = registo.get("resposta");
            return resposta != null && resposta.isObject() ? resposta : null;
        } catch (IOException e) {
            return null;
        }
    }

    public void guardar(String chave, String modelo, JsonNode resposta) {
        Map<String, Object> registo = new LinkedHashMap<>();
        registo.put("chave", chave);
        registo.put("modelo", modelo);
        registo.put("quando", Instant.now().toString());
        registo.put("resposta", resposta);
        try {
            dataFiles.escreverJson(caminho(chave), registo);
        } catch (IOException e) {
            // cache é acessório: não bloquear a leitura por causa dele
        }
    }

    private Path caminho(String chave) {
        return dataFiles.subpasta("cache").resolve(chave + ".json");
    }

    private static String resumoSha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
