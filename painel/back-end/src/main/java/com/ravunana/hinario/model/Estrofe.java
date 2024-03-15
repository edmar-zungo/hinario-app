package com.ravunana.hinario.model;


import java.util.Objects;
import java.util.UUID;


public class Estrofe {

    private UUID id;

    private long numero;

    private String descricao;

    private boolean isRefrao;
    private Hino hino;

    public Estrofe() {
    }

    public Estrofe(UUID id, long numero, String descricao, boolean isRefrao, Hino hino) {
        this.id = id;
        this.numero = numero;
        this.descricao = descricao;
        this.isRefrao = isRefrao;
        this.hino = hino;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public long getNumero() {
        return numero;
    }

    public void setNumero(long numero) {
        this.numero = numero;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isRefrao() {
        return isRefrao;
    }

    public void setRefrao(boolean refrao) {
        isRefrao = refrao;
    }

    public Hino getHino() {
        return hino;
    }

    public void setHino(Hino hino) {
        this.hino = hino;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Estrofe estrofe = (Estrofe) o;
        return numero == estrofe.numero && isRefrao == estrofe.isRefrao && Objects.equals(id, estrofe.id) && Objects.equals(descricao, estrofe.descricao) && Objects.equals(hino, estrofe.hino);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, numero, descricao, isRefrao, hino);
    }

    @Override
    public String toString() {
        return "Estrofe{" +
                "id=" + id +
                ", numero=" + numero +
                ", descricao='" + descricao + '\'' +
                ", isRefrao=" + isRefrao +
                ", hinoId=" + hino +
                '}';
    }
}
