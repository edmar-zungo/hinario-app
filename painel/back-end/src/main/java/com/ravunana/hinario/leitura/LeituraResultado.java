package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Leitura bem-sucedida de uma página por um fornecedor.
 *
 * {@code tentativas} guarda a rastreabilidade pedida no PRD 5.8.7
 * ({@code ok|429|erro|bloqueado}); {@code origem} distingue resposta da API
 * de resposta servida pelo cache.
 */
public record LeituraResultado(
        String fornecedor,
        String modelo,
        List<Tentativa> tentativas,
        JsonNode resposta) {

    /** Registo de uma tentativa de leitura (PRD 3.2 e 5.8.7). */
    public record Tentativa(String modelo, String resultado, String quando, String origem) {

        public static Tentativa api(String modelo, String resultado) {
            return new Tentativa(modelo, resultado, java.time.Instant.now().toString(), "api");
        }

        public static Tentativa cache(String modelo, String resultado) {
            return new Tentativa(modelo, resultado, java.time.Instant.now().toString(), "cache");
        }
    }
}
