package com.ravunana.hinario.exportacao;

import java.util.List;

/**
 * Resultado da validação de um {@link HinarioNovo}.
 *
 * {@code erros} impedem a exportação; {@code avisos} são mostrados no painel e
 * no relatório da migração mas não bloqueiam (PRD secção 5.7: assinalar, não
 * remover).
 */
public record ResultadoValidacao(List<String> erros, List<String> avisos) {

    public boolean valido() {
        return erros.isEmpty();
    }
}
