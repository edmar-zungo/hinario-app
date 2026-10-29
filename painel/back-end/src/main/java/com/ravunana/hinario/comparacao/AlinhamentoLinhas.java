package com.ravunana.hinario.comparacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Alinhamento linha a linha por distância de edição (PRD 5.7.3): programação
 * dinâmica estilo Needleman-Wunsch, com emparelhamento fraco penalizado para
 * não forçar juntar linhas que nada têm a ver.
 */
public class AlinhamentoLinhas {

    /** Par alinhado: índices em cada lado; -1 = linha só de um dos lados. */
    public record Par(int gemini, int tesseract) {
    }

    /** Só se emparelha a partir desta semelhança; abaixo, são omissões de um lado. */
    static final double LIMITE = 0.5;

    /** Custo de cada linha que não encontra par (inserção/omissão). */
    static final double OMISSAO = 0.2;

    /** Devolve os pares pela ordem das linhas do Gemini. */
    public List<Par> alinhar(List<String> gemini, List<String> tesseract) {
        int n = gemini.size();
        int m = tesseract.size();
        double[][] pontos = new double[n + 1][m + 1];
        int[][] veio = new int[n + 1][m + 1]; // 0 = diagonal, 1 = cima, 2 = esquerda
        for (int i = 1; i <= n; i++) {
            pontos[i][0] = pontos[i - 1][0] - OMISSAO;
            veio[i][0] = 1;
        }
        for (int j = 1; j <= m; j++) {
            pontos[0][j] = pontos[0][j - 1] - OMISSAO;
            veio[0][j] = 2;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double sim = similaridade(gemini.get(i - 1), tesseract.get(j - 1));
                double diagonal = pontos[i - 1][j - 1] + (sim >= LIMITE ? sim : sim - 1.0);
                double cima = pontos[i - 1][j] - OMISSAO;
                double esquerda = pontos[i][j - 1] - OMISSAO;
                double melhor = Math.max(diagonal, Math.max(cima, esquerda));
                pontos[i][j] = melhor;
                // em empate, prefere-se a diagonal e depois consumir do Tesseract,
                // para as linhas só do Gemini saírem primeiro na lista
                veio[i][j] = melhor == diagonal ? 0 : melhor == esquerda ? 2 : 1;
            }
        }
        List<Par> pares = new ArrayList<>();
        int i = n;
        int j = m;
        while (i > 0 || j > 0) {
            int direcao = veio[i][j];
            if (direcao == 0) {
                pares.add(new Par(i - 1, j - 1));
                i--;
                j--;
            } else if (direcao == 1) {
                pares.add(new Par(i - 1, -1));
                i--;
            } else {
                pares.add(new Par(-1, j - 1));
                j--;
            }
        }
        java.util.Collections.reverse(pares);
        return pares;
    }

    /** 1 - distância de edição / maior comprimento (1 se as duas vazias). */
    static double similaridade(String a, String b) {
        int dist = distanciaEdicao(a == null ? "" : a, b == null ? "" : b);
        int maior = Math.max(a == null ? 0 : a.length(), b == null ? 0 : b.length());
        return maior == 0 ? 1.0 : 1.0 - (double) dist / maior;
    }

    /** Distância de Levenshtein clássica (memória em duas linhas). */
    static int distanciaEdicao(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] atual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            atual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
            }
            int[] troca = anterior;
            anterior = atual;
            atual = troca;
        }
        return anterior[b.length()];
    }
}
