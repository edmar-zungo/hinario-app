package com.ravunana.hinario.comparacao;

import com.ravunana.hinario.leitura.LeituraExcecao;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Execução do binário Tesseract numa imagem. Separado do
 * {@link TesseractProvider} para os testes poderem substituir o motor.
 */
public interface TesseractComando {

    /**
     * Corre o Tesseract sobre a imagem e devolve a saída TSV.
     *
     * @throws LeituraExcecao se o binário falhar, não existir ou exceder o timeout
     * @throws IOException se não for possível executar
     */
    String executar(Path imagem, TesseractConfig config) throws IOException, InterruptedException, LeituraExcecao;
}
