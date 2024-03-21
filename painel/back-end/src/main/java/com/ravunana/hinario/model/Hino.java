package com.ravunana.hinario.model;



import com.ravunana.hinario.enums.Linguas;

import java.util.Objects;
import java.util.UUID;


public class Hino {

    private UUID id;

    private String titulo;

    private long pagina;

    private long numero;

    private String autor;

    private String dataCriacao;
    private String dataActualizacao;

    private String comentario;

    private boolean isFavorito;
    private Linguas lingua;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public long getPagina() {
        return pagina;
    }

    public void setPagina(long pagina) {
        this.pagina = pagina;
    }

    public long getNumero() {
        return numero;
    }

    public void setNumero(long numero) {
        this.numero = numero;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(String dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public String getDataActualizacao() {
        return dataActualizacao;
    }

    public void setDataActualizacao(String dataActualizacao) {
        this.dataActualizacao = dataActualizacao;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public boolean getIsFavorito() {
        return isFavorito;
    }

    public void setFavorito(boolean favorito) {
        isFavorito = favorito;
    }
    public Linguas getLingua() {
        return lingua;
    }

    public void setLingua(Linguas lingua) {
        this.lingua = lingua;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hino hino = (Hino) o;
        return pagina == hino.pagina && numero == hino.numero && isFavorito == hino.isFavorito && Objects.equals(id, hino.id) && Objects.equals(titulo, hino.titulo) && Objects.equals(autor, hino.autor) && Objects.equals(dataCriacao, hino.dataCriacao) && Objects.equals(dataActualizacao, hino.dataActualizacao) && Objects.equals(comentario, hino.comentario) && Objects.equals(lingua, hino.lingua);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, pagina, numero, autor, dataCriacao, dataActualizacao, comentario, isFavorito, lingua);
    }

    @Override
    public String toString() {
        return "Hino{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", pagina=" + pagina +
                ", numero=" + numero +
                ", autor='" + autor + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", dataActualizacao=" + dataActualizacao +
                ", comenatrio='" + comentario + '\'' +
                ", isFavorito=" + isFavorito +
                ", lingua='" + lingua + '\'' +
                '}';
    }


}
