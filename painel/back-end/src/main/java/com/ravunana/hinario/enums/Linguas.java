package com.ravunana.hinario.enums;

/**
 * Línguas do hinário. O código (minúsculo) identifica o hinário no esquema
 * novo: {@code hinario-<codigo>.json} (ver docs/esquema-export.md).
 */
public enum Linguas {
    PORTUGUES("por", "Hinário Português"),
    KIKONGO_REVISADO("kkr", "Hinário Kikongo (revisado)"),
    KIKONGO_ORIGEM("kko", "Hinário Kikongo (origem)"),
    KINBUMDU("kmb", "Hinário Kimbundu");

    private final String codigo;
    private final String nome;

    Linguas(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public static Linguas porCodigo(String codigo) {
        for (Linguas lingua : values()) {
            if (lingua.codigo.equalsIgnoreCase(codigo)) {
                return lingua;
            }
        }
        throw new IllegalArgumentException("Língua desconhecida: " + codigo);
    }
}
