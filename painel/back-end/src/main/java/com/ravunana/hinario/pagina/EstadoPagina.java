package com.ravunana.hinario.pagina;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Estados do ciclo de vida de uma página (PRD secção 3.2 e 7).
 *
 * O valor gravado em {@code paginas/<n>.json} é o texto minúsculo.
 */
public enum EstadoPagina {
    IMPORTADA("importada"),
    EM_FILA("em_fila"),
    LIDA("lida"),
    COMPARADA("comparada"),
    ESTRUTURADA("estruturada"),
    EM_REVISAO("em_revisao"),
    CONFIRMADA("confirmada"),
    PAUSADA_QUOTA("pausada_quota"),
    ERRO_LEITURA("erro_leitura"),
    BLOQUEADA_PELA_IA("bloqueada_pela_ia");

    private final String valor;

    EstadoPagina(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoPagina porValor(String valor) {
        for (EstadoPagina estado : values()) {
            if (estado.valor.equals(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de página desconhecido: " + valor);
    }
}
