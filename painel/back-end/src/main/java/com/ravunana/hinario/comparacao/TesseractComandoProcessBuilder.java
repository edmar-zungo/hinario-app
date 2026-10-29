package com.ravunana.hinario.comparacao;

import com.ravunana.hinario.leitura.LeituraExcecao;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@code tesseract <imagem> stdout -l <idioma> --psm <n> tsv} por
 * {@code ProcessBuilder} — sem dependências nativas no projeto (o motor é
 * requisito da máquina, como o Java e o Maven).
 */
@Component
public class TesseractComandoProcessBuilder implements TesseractComando {

    @Override
    public String executar(Path imagem, TesseractConfig config) throws LeituraExcecao {
        List<String> comando = new ArrayList<>(List.of(
                config.getBinario(), imagem.toString(), "stdout",
                "-l", config.getIdioma(),
                "--psm", String.valueOf(config.getPsm()),
                "tsv"));
        ProcessBuilder construtor = new ProcessBuilder(comando);
        if (config.getDatapath() != null && !config.getDatapath().isBlank()) {
            construtor.environment().put("TESSDATA_PREFIX", config.getDatapath().trim());
        }
        Process processo;
        try {
            processo = construtor.start();
        } catch (IOException e) {
            throw new LeituraExcecao("não foi possível executar o Tesseract ("
                    + config.getBinario() + "): " + e.getMessage());
        }
        String erro;
        try (InputStream saida = processo.getInputStream(); InputStream erros = processo.getErrorStream()) {
            String tsv = new String(saida.readAllBytes(), StandardCharsets.UTF_8);
            erro = new String(erros.readAllBytes(), StandardCharsets.UTF_8);
            if (!processo.waitFor(config.getTimeoutSegundos(), TimeUnit.SECONDS)) {
                processo.destroyForcibly();
                throw new LeituraExcecao("Tesseract excedeu " + config.getTimeoutSegundos() + "s");
            }
            if (processo.exitValue() != 0) {
                throw new LeituraExcecao("Tesseract terminou com o código " + processo.exitValue()
                        + (erro.isBlank() ? "" : ": " + erro.trim()));
            }
            return tsv;
        } catch (InterruptedException e) {
            processo.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new LeituraExcecao("Tesseract interrompido");
        } catch (IOException e) {
            throw new LeituraExcecao("falha a ler a saída do Tesseract: " + e.getMessage());
        }
    }
}
