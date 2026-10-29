package com.ravunana.hinario.comparacao;

import java.text.Normalizer;
import java.util.Set;

/**
 * Classificação determinística de um par de linhas alinhadas (PRD 5.7.3):
 * {@code igual}, {@code so_acento}, {@code so_pontuacao} ou {@code divergente}.
 *
 * Só compara — nunca altera texto.
 */
public class ClassificadorLinha {

    /** Pontuação conhecida das páginas (PRD 5.1.2: só assinalar, nunca "corrigir"). */
    private static final Set<Character> PONTOS = Set.of(
            '.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']',
            '-', '–', '—', '…', '/', '’', '‘', '“', '”', '*', '«', '»');

    /** {@code igual} | {@code so_acento} | {@code so_pontuacao} | {@code divergente}. */
    public String classificar(String gemini, String tesseract) {
        String a = Normalizer.normalize(gemini == null ? "" : gemini, Normalizer.Form.NFC).trim();
        String b = Normalizer.normalize(tesseract == null ? "" : tesseract, Normalizer.Form.NFC).trim();
        if (a.equals(b)) {
            return "igual";
        }
        if (semAcentos(a).equals(semAcentos(b))) {
            return "so_acento";
        }
        if (semPontuacao(a).equals(semPontuacao(b))) {
            return "so_pontuacao";
        }
        return "divergente";
    }

    /** Remove só os diacríticos (NFD + marcas combinantes), mantendo o resto. */
    static String semAcentos(String texto) {
        return Normalizer.normalize(
                Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", ""),
                Normalizer.Form.NFC);
    }

    /** Remove só a pontuação conhecida (espaços e letras ficam). */
    static String semPontuacao(String texto) {
        StringBuilder ficam = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (!PONTOS.contains(c)) {
                ficam.append(c);
            }
        }
        return ficam.toString();
    }
}
