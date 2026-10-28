package com.ravunana.hinario.leitura;

import java.util.List;

/** Todos os modelos da cadeia estão pausados: a fila retoma sozinha (PRD 5.8.5). */
public class QuotaEsgotadaExcecao extends LeituraExcecao {

    public QuotaEsgotadaExcecao(List<LeituraResultado.Tentativa> tentativas) {
        super("quota esgotada em todos os modelos", tentativas);
    }
}
