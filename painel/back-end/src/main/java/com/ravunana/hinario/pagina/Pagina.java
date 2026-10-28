package com.ravunana.hinario.pagina;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Ficheiro de trabalho de uma página (PRD secção 3.2), gravado em
 * {@code paginas/<n>.json}.
 *
 * Os caminhos de imagem são relativos à pasta de dados. {@code leituras},
 * {@code divergencias} e {@code blocos} ficam vazios na importação e são
 * preenchidos nas fases seguintes (Gemini, Tesseract, revisão).
 */
public record Pagina(
        int pagina,
        String hinarioId,
        String imagem,
        String imagemProcessada,
        String hash,
        EstadoPagina estado,
        String nomeOriginal,
        String importadoEm,
        int largura,
        int altura,
        JsonNode leituras,
        JsonNode divergencias,
        JsonNode blocos) {

    /** Cópia com outro estado (o registo é imutável). */
    public Pagina comEstado(EstadoPagina novo) {
        return new Pagina(pagina, hinarioId, imagem, imagemProcessada, hash, novo,
                nomeOriginal, importadoEm, largura, altura, leituras, divergencias, blocos);
    }

    /** Cópia com as leituras actualizadas (`leituras.gemini`, `leituras.tesseract`). */
    public Pagina comLeituras(JsonNode novas) {
        return new Pagina(pagina, hinarioId, imagem, imagemProcessada, hash, estado,
                nomeOriginal, importadoEm, largura, altura, novas, divergencias, blocos);
    }
}
