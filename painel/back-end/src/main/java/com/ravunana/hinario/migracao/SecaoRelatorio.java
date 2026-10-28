package com.ravunana.hinario.migracao;

import java.util.List;

/**
 * Uma secção do relatório {@code migracao.md}.
 *
 * @param titulo     título da secção (sem {@code ##})
 * @param encontrados número de ocorrências encontradas (0 = nada a assinalar)
 * @param linhas     linhas em markdown (bullets); só quando há ocorrências
 */
public record SecaoRelatorio(String titulo, int encontrados, List<String> linhas) {

    public static SecaoRelatorio vazia(String titulo) {
        return new SecaoRelatorio(titulo, 0, List.of());
    }
}
