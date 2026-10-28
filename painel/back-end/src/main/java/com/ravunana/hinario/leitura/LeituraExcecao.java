package com.ravunana.hinario.leitura;

import java.util.List;

/**
 * Falha definitiva de leitura de uma página. Carrega as tentativas feitas,
 * para o ficheiro da página as registar mesmo em erro (PRD 5.8.7).
 */
public class LeituraExcecao extends Exception {

    private final List<LeituraResultado.Tentativa> tentativas;

    public LeituraExcecao(String mensagem, List<LeituraResultado.Tentativa> tentativas) {
        super(mensagem);
        this.tentativas = tentativas == null ? List.of() : List.copyOf(tentativas);
    }

    public List<LeituraResultado.Tentativa> getTentativas() {
        return tentativas;
    }
}
