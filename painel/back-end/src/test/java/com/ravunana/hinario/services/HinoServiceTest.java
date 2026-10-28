package com.ravunana.hinario.services;

import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.model.Hino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HinoServiceTest {

    @TempDir
    Path temp;

    private DataFiles dataFiles;

    @BeforeEach
    void preparar() throws IOException {
        dataFiles = new DataFiles(temp.toString());
        Files.writeString(temp.resolve("hinario.json"), """
                [ {
                  "id" : "68489a40-16eb-49a0-8e80-ad93dd5606da",
                  "titulo" : "Ouço a benigna voz",
                  "pagina" : 22,
                  "numero" : 2,
                  "autor" : "IERA",
                  "dataCriacao" : "2024-03-21",
                  "dataActualizacao" : "2024-06-29",
                  "comentario" : "nenhum",
                  "isFavorito" : false,
                  "lingua" : "PORTUGUES"
                } ]
                """);
    }

    @Test
    void criarHinoSemAutorNaoLancaErro() {
        HinoService servico = new HinoService(dataFiles);
        Hino hino = new Hino();
        hino.setTitulo("Novo hino");
        hino.setPagina(1);
        hino.setNumero(1);
        hino.setLingua(Linguas.PORTUGUES);
        hino.setAutor(null);
        hino.setComentario(null);

        assertDoesNotThrow(() -> servico.createHino(hino), "getAutor().equals(null) rebentava com NPE");
        assertEquals("IERA", hino.getAutor());
        assertEquals("Nenhum comenatario adicionado", hino.getComentario());
    }

    @Test
    void pesquisaNaoDependeDeAcentos() throws IOException {
        HinoService servico = new HinoService(dataFiles);

        assertEquals(1, servico.pesquisa("ouco").size(), "Ouço sem cedilha/cedo deve encontrar");
        assertEquals(1, servico.pesquisa("BENIGNA").size(), "pesquisa deve ignorar maiúsculas");
        assertEquals(1, servico.pesquisa("benigna voz").size());
        assertEquals(0, servico.pesquisa("inexistente").size());
        assertEquals(1, servico.pesquisa("22").size(), "pesquisa por número de página/hino");
    }

    @Test
    void escreverEEAtomicoEAtualizaOFicheiro() throws IOException {
        HinoService servico = new HinoService(dataFiles);
        Hino hino = new Hino();
        hino.setTitulo("Outro título");
        hino.setPagina(2);
        hino.setNumero(2);
        hino.setLingua(Linguas.PORTUGUES);

        servico.createHino(hino);

        String conteudo = Files.readString(temp.resolve("hinario.json"));
        assertFalse(conteudo.contains(".tmp"), "não devem restar ficheiros temporários");
        assertEquals(2, conteudo.split("\"id\"", -1).length - 1);
        List<String> temporarios;
        try (var stream = Files.list(temp)) {
            temporarios = stream.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".tmp")).toList();
        }
        assertEquals(List.of(), temporarios);
    }
}
