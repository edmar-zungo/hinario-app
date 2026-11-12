package com.ravunana.hinario.leitura;

import java.util.List;

/**
 * Todos os modelos bloquearam a resposta: a página fica
 * {@code bloqueada_pela_ia} até uma releitura manual.
 */
public class BloqueadaExcecao extends LeituraExcecao {

    public BloqueadaExcecao(List<LeituraResultado.Tentativa> tentativas) {
        super("resposta bloqueada em todos os modelos", tentativas);
    }
}
