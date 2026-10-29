package com.ravunana.hinario.comparacao;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.leitura.LeituraExcecao;
import com.ravunana.hinario.leitura.LeituraResultado;
import com.ravunana.hinario.leitura.OcrProvider;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Segundo leitor do PRD (secção 2, Fase 4): corre o Tesseract sobre a imagem
 * processada e devolve linhas com confiança ({@code leituras.tesseract}).
 *
 * A saída TSV é agrupada por linha; a confiança da linha é a média das
 * palavras, de 0 a 1.
 */
@Component
public class TesseractProvider implements OcrProvider {

    private final TesseractComando comando;
    private final TesseractConfig config;
    private final DataFiles dataFiles;

    public TesseractProvider(TesseractComando comando, TesseractConfig config, DataFiles dataFiles) {
        this.comando = comando;
        this.config = config;
        this.dataFiles = dataFiles;
    }

    @Override
    public String nome() {
        return "tesseract";
    }

    @Override
    public boolean disponivel() {
        return config.disponivel();
    }

    @Override
    public LeituraResultado ler(Pagina pagina) throws LeituraExcecao {
        Path imagem = imagemParaLer(pagina);
        String tsv;
        try {
            tsv = comando.executar(imagem, config);
        } catch (IOException e) {
            throw new LeituraExcecao("Tesseract não executou: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LeituraExcecao("Tesseract interrompido");
        }
        List<LinhaLida> linhas = parseTsv(tsv);
        ObjectNode resposta = dataFiles.mapper().createObjectNode();
        ArrayNode array = resposta.putArray("linhas");
        for (LinhaLida linha : linhas) {
            ObjectNode registo = array.addObject();
            registo.put("texto", linha.texto());
            registo.put("confianca", linha.confianca());
        }
        String modelo = "tesseract-" + config.getIdioma();
        String agora = Instant.now().toString();
        return new LeituraResultado("tesseract", modelo,
                List.of(new LeituraResultado.Tentativa(modelo, "ok", agora, "local")), resposta);
    }

    /** Prefere a imagem processada (2000 px); cai para o original se faltar. */
    private Path imagemParaLer(Pagina pagina) throws LeituraExcecao {
        Path processada = dataFiles.resolve(pagina.imagemProcessada());
        Path imagem = Files.isRegularFile(processada) ? processada : dataFiles.resolve(pagina.imagem());
        if (!Files.isRegularFile(imagem)) {
            throw new LeituraExcecao("imagem em falta para a página " + pagina.pagina());
        }
        return imagem;
    }

    /** Uma linha lida: texto e confiança média das palavras (0 a 1). */
    record LinhaLida(String texto, double confianca) {
    }

    /**
     * Converte o TSV do Tesseract em linhas. Colunas do TSV:
     * {@code level page_num block_num par_num line_num word_num left top width height conf text};
     * só as linhas de palavra (level 5, conf >= 0, texto não vazio) contam.
     */
    static List<LinhaLida> parseTsv(String tsv) {
        Map<String, List<Double>> confiancasPorLinha = new LinkedHashMap<>();
        Map<String, List<String>> palavrasPorLinha = new LinkedHashMap<>();
        String[] linhasTsv = tsv == null ? new String[0] : tsv.split("\r?\n", -1);
        for (int i = 1; i < linhasTsv.length; i++) { // a linha 0 é o cabeçalho
            String[] colunas = linhasTsv[i].split("\t", -1);
            if (colunas.length < 12) {
                continue;
            }
            String nivel = colunas[0];
            String conf = colunas[10];
            String texto = colunas[11].trim();
            if (!"5".equals(nivel) || texto.isEmpty()) {
                continue;
            }
            double valor;
            try {
                valor = Double.parseDouble(conf);
            } catch (NumberFormatException e) {
                continue;
            }
            if (valor < 0) {
                continue;
            }
            String chave = colunas[2] + ":" + colunas[3] + ":" + colunas[4];
            confiancasPorLinha.computeIfAbsent(chave, k -> new ArrayList<>()).add(valor);
            palavrasPorLinha.computeIfAbsent(chave, k -> new ArrayList<>()).add(texto);
        }
        List<LinhaLida> linhas = new ArrayList<>();
        for (Map.Entry<String, List<String>> entrada : palavrasPorLinha.entrySet()) {
            List<Double> confs = confiancasPorLinha.get(entrada.getKey());
            double soma = 0;
            for (double c : confs) {
                soma += c;
            }
            // a confiança do Tesseract é 0-100; guardamos 0-1 (PRD 3.2)
            double media = confs.isEmpty() ? 0
                    : Math.round((soma / confs.size()) / 100.0 * 100.0) / 100.0;
            linhas.add(new LinhaLida(String.join(" ", entrada.getValue()).trim(), media));
        }
        return linhas;
    }
}
