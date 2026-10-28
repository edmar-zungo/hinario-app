package com.ravunana.hinario.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.model.Hino;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HinoService {

    private final Path caminho;
    private final DataFiles dataFiles;

    private final ObjectMapper mapper = new ObjectMapper();

    List<Hino> hinos;
    List<Hino> hinosFavoritos;

    public HinoService(DataFiles dataFiles){
        this.dataFiles = dataFiles;
        this.caminho = dataFiles.resolve("hinario.json");
        this.loadHinos();
        this.loadHinosFavoritos();
    }

    public List<Hino> getAllHinos() throws IOException {

        return hinos;
    }

    public Hino getHinoById(UUID hinoId){
        return hinos.stream().filter(x -> x.getId().equals(hinoId)).findAny().orElseThrow( () -> new RuntimeException("O hino não existe!"));
    }

    public Hino getHinoByPagina(long pagina){
        return hinos.stream().filter(x -> x.getPagina() == pagina).findAny().orElseThrow( () -> new RuntimeException("O hino não existe!"));
    }
    public Hino getHinoByNumero(long numero){
        return hinos.stream().filter(x -> x.getNumero() == numero).findAny().orElseThrow( () -> new RuntimeException("O hino não existe!"));
    }

    public Hino createHino(Hino hino) {
        hino.setId(UUID.randomUUID());
        hino.setDataCriacao(LocalDate.now().toString());
        hino.setDataActualizacao(LocalDate.now().toString());



        if (hino.getAutor() == null || hino.getAutor().isBlank()){
            hino.setAutor("IERA");
        }

        if (hino.getComentario() == null || hino.getComentario().isBlank()){
            hino.setComentario("Nenhum comenatario adicionado");
        }

        hinos.add(hino);
        saveToFile();

        if (hino.getIsFavorito()){
            this.loadHinosFavoritos();
        }

        return hino;
    }

    public Hino updateHino(UUID hinoId, Hino hino){
        Hino hinoResult = this.getHinoById(hinoId);

        if (hinoResult != null){
            hinoResult.setId(hinoId);
            hinoResult.setDataCriacao(hino.getDataCriacao());
            hinoResult.setDataActualizacao(LocalDate.now().toString());
            hinoResult.setComentario(hino.getComentario());
            hinoResult.setAutor(hino.getAutor());
            hinoResult.setFavorito(hino.getIsFavorito());
            hinoResult.setPagina(hino.getPagina());
            hinoResult.setNumero(hino.getNumero());
            hinoResult.setTitulo(hino.getTitulo());
            hinoResult.setLingua(hino.getLingua());

            saveToFile();

            if (!hino.getIsFavorito()){
                hinosFavoritos.removeIf(x -> x.getId().equals(hinoId));
                this.loadHinosFavoritos();
                this.loadHinos();
            }

            if (hino.getIsFavorito()){
                this.loadHinosFavoritos();
                this.loadHinos();
            }
        }

        return hino;
    }

    public void deleteHino(UUID hinoId){
        hinos.removeIf(x -> x.getId().equals(hinoId));
        hinosFavoritos.removeIf(x -> x.getId().equals(hinoId));

        saveToFile();
        this.loadHinosFavoritos();
        this.loadHinos();
    }

    private Path criarArquivo(Path caminho) throws IOException {
        if (!Files.exists(caminho)) {
            dataFiles.escreverTexto(caminho, "[]");
        }
        return caminho;
    }

    private void saveToFile(){
        try {
            dataFiles.escreverJson(caminho, hinos);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void loadHinos(){
        try {
            Path arquivo = criarArquivo(caminho);
            if (Files.exists(arquivo)){
                if (Files.size(arquivo) == 0){
                    Collections.emptyList();
                }
                hinos = mapper.readValue(arquivo.toFile(), new TypeReference<List<Hino>>() {});
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }


/**
 * LOGICA DOS HINOS FAVORITOS
 * */

public List<Hino> getAllFavoritos(){
    return hinosFavoritos;
}

    public Hino addOrRemoveHinoFavorito(UUID hinoId) {

        Hino hino = this.getHinoById(hinoId);

        if (hino.getIsFavorito()){
            hino.setFavorito(false);
            saveToFile();

            this.updateHino(hinoId, hino);
            this.loadHinos();

            return hino;
        }

        hino.setFavorito(true);

        saveToFile();

        this.updateHino(hinoId, hino);
        this.loadHinos();

        return hino;
    }

    private void loadHinosFavoritos() {
    try {
        hinosFavoritos = this.getAllHinos().stream().filter(Hino::getIsFavorito).collect(Collectors.toList());
    } catch (IOException e){
        e.printStackTrace();
    }
    }

    public List<Hino> pesquisa(String query) throws IOException {
        String termo = semAcentos(query);
        if (termo.isEmpty()) {
            return this.getAllHinos();
        }

        return this.getAllHinos().stream()
                .filter(x -> semAcentos(x.getTitulo()).contains(termo)
                        || String.valueOf(x.getNumero()).contains(termo)
                        || String.valueOf(x.getPagina()).contains(termo))
                .toList();
    }

    /** Minúsculas e sem diacríticos: a pesquisa não depende de acentos. */
    static String semAcentos(String texto) {
        if (texto == null) {
            return "";
        }
        String semMarcas = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semMarcas.toLowerCase(Locale.ROOT).trim();
    }

}
