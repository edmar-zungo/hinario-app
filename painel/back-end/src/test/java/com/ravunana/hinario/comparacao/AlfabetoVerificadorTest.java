package com.ravunana.hinario.comparacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.services.DataFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PRD 5.7.2: assinalar fora do alfabeto, nunca remover. */
class AlfabetoVerificadorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temp;

    private AlfabetoVerificador alfabeto;

    @BeforeEach
    void preparar() {
        alfabeto = new AlfabetoVerificador(new DataFiles(temp.toString()));
    }

    @Test
    void textoLimpoNaoTemMarcas() {
        assertTrue(alfabeto.marcar("M’ uhukunii aJesus — [?] kuanza 2").isEmpty());
    }

    @Test
    void letraForaDoAlfabetoLatinoEMarcada() {
        var marcas = alfabeto.marcar("abc§def");
        assertEquals(1, marcas.size());
        assertEquals(3, marcas.get(0).get("posicao").asInt());
        assertEquals("§", marcas.get(0).get("caractere").asText());
    }

    @Test
    void caracteresDeControloSaoMarcados() {
        // \b = backspace: carácter de controlo fora do alfabeto conhecido
        var marcas = alfabeto.marcar("linha\bquebrada");
        assertEquals(1, marcas.size());
        assertEquals(5, marcas.get(0).get("posicao").asInt());
    }

    @Test
    void acentosLatinosConhecidosPassam() {
        assertTrue(alfabeto.marcar("kínzu êndu õmbô ç").isEmpty());
    }

    @Test
    void verificarRespostaIncluiBlocoELinha() {
        ObjectNode raiz = MAPPER.createObjectNode();
        ObjectNode bloco = raiz.putArray("blocos").addObject();
        ArrayNode linhas = bloco.putArray("linhas");
        linhas.addObject().put("texto", "texto limpo").put("recuo", 0);
        linhas.addObject().put("texto", "mau§texto").put("recuo", 0);

        ArrayNode marcas = alfabeto.verificarResposta(raiz);
        assertEquals(1, marcas.size());
        assertEquals(0, marcas.get(0).get("bloco").asInt());
        assertEquals(1, marcas.get(0).get("linha").asInt());
        assertEquals(3, marcas.get(0).get("posicao").asInt());
        JsonNode marca = marcas.get(0);
        assertEquals("§", marca.get("caractere").asText());
    }

    @Test
    void respostaNulaNaoExplode() {
        assertTrue(alfabeto.verificarResposta(null).isEmpty());
    }
}
