package com.ravunana.hinario.model;


import java.util.Objects;
import java.util.UUID;

public class Favoritos {

    private UUID id;

    private Hino hino;

    public Favoritos() {
    }

    public Favoritos(UUID id, Hino hino) {
        this.id = id;
        this.hino = hino;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
        Favoritos favoritos = (Favoritos) o;
        return Objects.equals(id, favoritos.id) && Objects.equals(hino, favoritos.hino) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Favoritos{" +
                "id=" + id +
                "hino=" + hino +
                '}';
    }


}
