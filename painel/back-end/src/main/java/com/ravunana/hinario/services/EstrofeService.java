package com.ravunana.hinario.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.model.Estrofe;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class EstrofeService {
    private final Path caminho;
    private final DataFiles dataFiles;
    private final ObjectMapper mapper = new ObjectMapper();
    List<Estrofe> estrofes;

    public EstrofeService(DataFiles dataFiles) {
        this.dataFiles = dataFiles;
        this.caminho = dataFiles.resolve("estrofes.json");
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

    private Path criarArquivo(Path caminho) throws IOException {
        if (!Files.exists(caminho)) {
            dataFiles.escreverTexto(caminho, "[]");
        }
        return caminho;
    }

    private void saveToFile(){
        try {
            dataFiles.escreverJson(caminho, estrofes);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void loadEstrofes(){
        try {
            Path arquivo = criarArquivo(caminho);
            if (Files.exists(arquivo)){
                if (Files.size(arquivo) == 0){
                    Collections.emptyList();
                }
                estrofes = mapper.readValue(arquivo.toFile(), new TypeReference<List<Estrofe>>() {});
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
