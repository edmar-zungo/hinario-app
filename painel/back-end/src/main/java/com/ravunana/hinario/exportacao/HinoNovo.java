package com.ravunana.hinario.exportacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Um hino do esquema novo.
 *
 * O {@code id} mantém o UUID do JSON atual, para que os favoritos guardados
 * nas apps continuem a apontar para o mesmo hino.
 *
 * @param blocos estrofes e coros na ordem impressa (PRD secção 3.3)
 */
public record HinoNovo(
        @NotNull UUID id,
        @NotNull Long numero,
        @NotNull Long pagina,
        @NotBlank String titulo,
        String autor,
        @NotNull @Valid List<BlocoNovo> blocos) {
}
