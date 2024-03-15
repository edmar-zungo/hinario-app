package com.ravunana.hinario.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.model.Estrofe;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class EstrofeService {
    private final String caminho = "/home/edmar-zungo/Documents/estrofes.json";
    private final ObjectMapper mapper = new ObjectMapper();
    List<Estrofe> estrofes;

    public EstrofeService() {
        loadEstrofes();
    }

    public List<Estrofe> getAllEstrofes() throws IOException {
        return estrofes;
    }

    public Estrofe getEstrofeById(UUID estrofeId){
        return estrofes.stream().filter(x -> x.getId().equals(estrofeId)).findAny().orElseThrow( () -> new RuntimeException("A estrofe não existe!"));
    }

    public Estrofe createEstrofe(Estrofe estrofe) {
        estrofe.setId(UUID.randomUUID());

        estrofes.add(estrofe);

        saveToFile();

        return estrofe;
    }

    public Estrofe updateEstrofe(UUID estrofeId, Estrofe estrofe){
        Estrofe estrofeResult = getEstrofeById(estrofeId);
        if (estrofeResult != null){
            estrofeResult.setId(estrofeId);
            estrofeResult.setHino(estrofe.getHino());
            estrofeResult.setDescricao(estrofe.getDescricao());
            estrofeResult.setNumero(estrofe.getNumero());
            estrofeResult.setRefrao(estrofe.isRefrao());

            saveToFile();
        }

        return estrofe;
    }

    public void deleteHino(UUID estrofeId){
        estrofes.removeIf(x -> x.getId().equals(estrofeId));
        saveToFile();
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
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(caminho), estrofes);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void loadEstrofes(){
        try {
            File arquivo = criarArquivo(caminho);
            if (arquivo.exists()){
                if (arquivo.length() == 0){
                    Collections.emptyList();
                }
                estrofes = mapper.readValue(arquivo, new TypeReference<List<Estrofe>>() {});
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
