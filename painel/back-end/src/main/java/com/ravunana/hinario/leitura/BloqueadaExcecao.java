package com.ravunana.hinario.leitura;

import java.util.List;

/**
 * Todos os modelos bloquearam a resposta: a página fica
 * {@code bloqueada_pela_ia} e na Fase 4 segue só com Tesseract (PRD 5.6).
 */
public class BloqueadaExcecao extends LeituraExcecao {

    public BloqueadaExcecao(List<LeituraResultado.Tentativa> tentativas) {
        super("resposta bloqueada em todos os modelos", tentativas);
    }
}
