package com.ravunana.hinario.exportacao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Uma linha impressa.
 *
 * @param texto  texto exatamente como impresso (Unicode NFC). Pode ser vazio
 *               quando a linha impressa está em branco: sinal de dúvida para a
 *               revisão, nunca é removido nem alterado.
 * @param recuo  0 = margem normal, 1 = recuada, 2 = mais recuada
 */
public record LinhaNovo(
        @NotNull String texto,
        @NotNull @Min(0) @Max(2) Integer recuo) {

    public static LinhaNovo de(String texto) {
        return new LinhaNovo(texto, 0);
    }
}
