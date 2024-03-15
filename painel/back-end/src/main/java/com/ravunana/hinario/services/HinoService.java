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

@Service
public class HinoService {

    private final String caminho = "/home/edmar-zungo/Documents/hinario.json";

    private final ObjectMapper mapper = new ObjectMapper();
    List<Hino> hinos;

    public HinoService() {
        loadHinos();
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

        if (!hino.isFavorito()){
            hino.setFavorito(false);
        }

        if (hino.getAutor().equals(null) || hino.getAutor().equals("")){
            hino.setAutor("IERA");
        }

        if (hino.getComentario().equals(null) || hino.getComentario().equals("")){
            hino.setComentario("Nenhum comenatario adicionado");
        }

//        hino.setAutor(hino.getAutor() == null ? "IERA" : hino.getAutor());
//        hino.setComentario( hino.getComentario() == null ? "Nenhum comentario adicionado" : hino.getComentario() );

        hinos.add(hino);

        saveToFile();

        return hino;
    }

    public Hino updateHino(UUID hinoId, Hino hino){
        Hino hinoResult = getHinoById(hinoId);
        if (hinoResult != null){
            hinoResult.setId(hinoId);
            hinoResult.setDataCriacao(hino.getDataCriacao());
            hinoResult.setDataActualizacao(LocalDate.now().toString());
            hinoResult.setComentario(hino.getComentario());
            hinoResult.setAutor(hino.getAutor());
            hinoResult.setFavorito(hino.isFavorito());
            hinoResult.setPagina(hino.getPagina());
            hinoResult.setNumero(hino.getNumero());
            hinoResult.setTitulo(hino.getTitulo());

            saveToFile();
        }

        return hino;
    }

    public boolean deleteHino(UUID hinoId){

        return hinos.removeIf(x -> x.getId().equals(hinoId));
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
}
