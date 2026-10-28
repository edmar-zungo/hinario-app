package com.ravunana.hinario.exportacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Esquema de saída {@code hinario-<id>.json} (PRD secção 3.3).
 * Ver {@code docs/esquema-export.md}.
 *
 * O texto nunca é alterado pela migração: as linhas são apenas cortadas nas
 * quebras de linha impressas ({@code \n}), sem espaços a mais nem a menos.
 *
 * @param id      código do hinário (por exemplo {@code kmb})
 * @param lingua  código da língua (igual a {@code id})
 * @param nome    nome legível do hinário
 * @param geradoEm data/hora UTC da geração (ISO-8601)
 * @param hinos   hinos por ordem de página e número
 */
public record HinarioNovo(
        @NotBlank String id,
        @NotBlank String lingua,
        @NotBlank String nome,
        String geradoEm,
        @NotNull @Valid List<HinoNovo> hinos) {
}
