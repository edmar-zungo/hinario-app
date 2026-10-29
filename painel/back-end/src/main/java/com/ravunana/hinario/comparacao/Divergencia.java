package com.ravunana.hinario.comparacao;

/**
 * Uma diferença entre as duas leituras (PRD 3.2): {@code bloco} e {@code linha}
 * apontam para a linha do Gemini; só no Tesseract, ficam a null.
 */
public record Divergencia(Integer bloco, Integer linha, String gemini, String tesseract, String tipo) {
}
