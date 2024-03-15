package com.ravunana.hinario.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.model.Favoritos;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.repositories.FavoritosRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class FavoritoService {

    private final String caminho = "/home/edmar-zungo/Documents/favoritos.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HinoService hinoService;
    List<Hino> hinos;

    public FavoritoService(HinoService hinoService) {
        this.hinoService = hinoService;
        loadHinos();
    }
    public List<Hino> getAllFavoritos(){
        return hinos;
    }

    public Hino createHinoFavorito(UUID hinoId) {

        Hino hino = hinoService.getHinoById(hinoId);
        hino.setFavorito(true);

        hinos.add(hino);

        saveToFile();

        this.hinoService.updateHino(hinoId, hino);

        return hino;
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
