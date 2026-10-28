package com.ravunana.hinario.exportacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Bloco de um hino no esquema novo.
 *
 * @param tipo    {@code estrofe} (numerada) ou {@code coro} (sem número)
 * @param numero  número da estrofe; {@code null} em coros
 * @param linhas  uma entrada por linha impressa, na ordem da página
 */
public record BlocoNovo(
        @NotBlank @Pattern(regexp = "estrofe|coro", message = "tipo deve ser 'estrofe' ou 'coro'")
        String tipo,
        Integer numero,
        @NotNull @Valid List<LinhaNovo> linhas) {

    public static BlocoNovo estrofe(int numero, List<LinhaNovo> linhas) {
        return new BlocoNovo("estrofe", numero, linhas);
    }

    public static BlocoNovo coro(List<LinhaNovo> linhas) {
        return new BlocoNovo("coro", null, linhas);
    }
}
