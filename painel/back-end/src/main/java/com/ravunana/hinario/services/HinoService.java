package com.ravunana.hinario.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.model.Hino;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HinoService {

    private final String caminho = "/home/edmar-zungo/Documents/hinario.json";

    private final ObjectMapper mapper = new ObjectMapper();

    List<Hino> hinos;
    List<Hino> hinosFavoritos;

    public HinoService(){
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


        if (hino.getAutor().equals(null) || hino.getAutor().equals("")){
            hino.setAutor("IERA");
        }

        if (hino.getComentario().equals(null) || hino.getComentario().equals("")){
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

    private File criarArquivo(String caminho) throws IOException {

        File file = new File(caminho);

        if (!file.exists()){
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write("[]");
            writer.flush();
            writer.close();
            System.out.println("Criado: " + file.createNewFile());
        } else {
            System.out.println("Existe: " + file.createNewFile());
        }

        return file;
    }

    private void saveToFile(){
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(caminho), hinos);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void loadHinos(){
        try {
            File arquivo = criarArquivo(caminho);
            if (arquivo.exists()){
                if (arquivo.length() == 0){
                    Collections.emptyList();
                }
                hinos = mapper.readValue(arquivo, new TypeReference<List<Hino>>() {});
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

    public Hino createHinoFavorito(UUID hinoId) {

        Hino hino = this.getHinoById(hinoId);
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

}
