package com.ravunana.hinario.comparacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Comparação Gemini vs Tesseract (PRD 5.7.3): alinha as linhas e devolve a
 * lista de divergências para o painel. **Qualquer diferença** é listada; as de
 * acento e pontuação levam o tipo próprio, porque são o risco que mais importa.
 */
@Component
public class ComparacaoService {

    private final AlinhamentoLinhas alinhamento;
    private final ClassificadorLinha classificador;
    private final DataFiles dataFiles;

    @org.springframework.beans.factory.annotation.Autowired
    public ComparacaoService(DataFiles dataFiles) {
        this(new AlinhamentoLinhas(), new ClassificadorLinha(), dataFiles);
    }

    /** Visível só para os testes substituírem a política de alinhamento. */
    ComparacaoService(AlinhamentoLinhas alinhamento, ClassificadorLinha classificador, DataFiles dataFiles) {
        this.alinhamento = alinhamento;
        this.classificador = classificador;
        this.dataFiles = dataFiles;
    }

    /** Divergências entre a resposta do Gemini (5.4) e {@code leituras.tesseract}. */
    public ArrayNode comparar(JsonNode respostaGemini, JsonNode leituraTesseract) {
        List<LinhaGemini> linhasGemini = achatar(respostaGemini);
        List<String> linhasTesseract = textoTesseract(leituraTesseract);

        List<String> textoGemini = new ArrayList<>();
        for (LinhaGemini linha : linhasGemini) {
            textoGemini.add(linha.texto());
        }

        ArrayNode saida = dataFiles.mapper().createArrayNode();
        for (AlinhamentoLinhas.Par par : alinhamento.alinhar(textoGemini, linhasTesseract)) {
            String ladoGemini = par.gemini() >= 0 ? linhasGemini.get(par.gemini()).texto() : null;
            String ladoTesseract = par.tesseract() >= 0 ? linhasTesseract.get(par.tesseract()) : null;
            String tipo = ladoGemini == null || ladoTesseract == null
                    ? "divergente" : classificador.classificar(ladoGemini, ladoTesseract);
            if ("igual".equals(tipo)) {
                continue;
            }
            Divergencia divergencia = new Divergencia(
                    par.gemini() >= 0 ? linhasGemini.get(par.gemini()).bloco() : null,
                    par.gemini() >= 0 ? linhasGemini.get(par.gemini()).linha() : null,
                    ladoGemini, ladoTesseract, tipo);
            ObjectNode registo = saida.addObject();
            if (divergencia.bloco() != null) {
                registo.put("bloco", divergencia.bloco());
            } else {
                registo.putNull("bloco");
            }
            if (divergencia.linha() != null) {
                registo.put("linha", divergencia.linha());
            } else {
                registo.putNull("linha");
            }
            if (divergencia.gemini() == null) {
                registo.putNull("gemini");
            } else {
                registo.put("gemini", divergencia.gemini());
            }
            if (divergencia.tesseract() == null) {
                registo.putNull("tesseract");
            } else {
                registo.put("tesseract", divergencia.tesseract());
            }
            registo.put("tipo", divergencia.tipo());
        }
        return saida;
    }

    /** Uma linha do Gemini, com a sua posição em {@code blocos}. */
    private record LinhaGemini(int bloco, int linha, String texto) {
    }

    /** Todas as linhas da resposta, pela ordem dos blocos. */
    static List<LinhaGemini> achatar(JsonNode respostaGemini) {
        List<LinhaGemini> linhas = new ArrayList<>();
        JsonNode blocos = respostaGemini == null ? null : respostaGemini.get("blocos");
        if (blocos == null || !blocos.isArray()) {
            return linhas;
        }
        for (int b = 0; b < blocos.size(); b++) {
            JsonNode linhasBloco = blocos.get(b).get("linhas");
            if (linhasBloco == null || !linhasBloco.isArray()) {
                continue;
            }
            for (int l = 0; l < linhasBloco.size(); l++) {
                linhas.add(new LinhaGemini(b, l, linhasBloco.get(l).path("texto").asText("")));
            }
        }
        return linhas;
    }

    static List<String> textoTesseract(JsonNode leituraTesseract) {
        List<String> linhas = new ArrayList<>();
        JsonNode array = leituraTesseract == null ? null : leituraTesseract.get("linhas");
        if (array == null || !array.isArray()) {
            return linhas;
        }
        for (JsonNode linha : array) {
            linhas.add(linha.path("texto").asText(""));
        }
        return linhas;
    }
}
