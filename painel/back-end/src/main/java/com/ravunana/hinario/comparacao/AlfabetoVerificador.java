package com.ravunana.hinario.comparacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Verificação do alfabeto (PRD 5.7.2): assinala caracteres fora do conjunto
 * esperado (letras latinas com diacríticos conhecidos, dígitos, pontuação
 * comum, {@code ’}, {@code -}). **Não remove nada** — só marca.
 */
@Component
public class AlfabetoVerificador {

    /** Pontuação comum das páginas; tudo o resto é assinalado. */
    private static final Set<Character> CONHECIDOS = Set.of(
            ' ', '.', ',', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']',
            '-', '–', '—', '…', '/', '’', '‘', '“', '”', '*', '«', '»',
            '\t', '\n');

    private final DataFiles dataFiles;

    public AlfabetoVerificador(DataFiles dataFiles) {
        this.dataFiles = dataFiles;
    }

    /** Marcas para todas as linhas da resposta do Gemini. */
    public ArrayNode verificarResposta(JsonNode respostaGemini) {
        ArrayNode marcas = dataFiles.mapper().createArrayNode();
        JsonNode blocos = respostaGemini == null ? null : respostaGemini.get("blocos");
        if (blocos == null || !blocos.isArray()) {
            return marcas;
        }
        for (int b = 0; b < blocos.size(); b++) {
            JsonNode linhasBloco = blocos.get(b).get("linhas");
            if (linhasBloco == null || !linhasBloco.isArray()) {
                continue;
            }
            for (int l = 0; l < linhasBloco.size(); l++) {
                String texto = linhasBloco.get(l).path("texto").asText("");
                for (ObjectNode marca : marcar(texto)) {
                    marca.put("bloco", b);
                    marca.put("linha", l);
                    marcas.add(marca);
                }
            }
        }
        return marcas;
    }

    /** Marcas numa linha: posição e carácter assinalado, em NFC. */
    List<ObjectNode> marcar(String texto) {
        List<ObjectNode> marcas = new ArrayList<>();
        if (texto == null) {
            return marcas;
        }
        String nfc = Normalizer.normalize(texto, Normalizer.Form.NFC);
        for (int i = 0; i < nfc.length(); i++) {
            char c = nfc.charAt(i);
            boolean letraLatina = Character.isLetter(c)
                    && Character.UnicodeScript.of(c) == Character.UnicodeScript.LATIN;
            if (letraLatina || Character.isDigit(c) || CONHECIDOS.contains(c)) {
                continue;
            }
            ObjectNode marca = dataFiles.mapper().createObjectNode();
            marca.put("posicao", i);
            marca.put("caractere", String.valueOf(c));
            marcas.add(marca);
        }
        return marcas;
    }
}
