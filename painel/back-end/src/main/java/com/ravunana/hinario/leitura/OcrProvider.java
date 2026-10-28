package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.pagina.Pagina;

import java.util.List;

/**
 * Um leitor do sistema (PRD secção 2): Gemini como principal, Tesseract como
 * segundo leitor (Fase 4).
 */
public interface OcrProvider {

    /** Nome do fornecedor: {@code gemini} ou {@code tesseract}. */
    String nome();

    /** Se o fornecedor está utilizável neste momento (chave/interruptor). */
    boolean disponivel();

    /**
     * Lê uma página inteira.
     *
     * @throws QuotaEsgotadaExcecao se todos os modelos estiverem pausados
     * @throws BloqueadaExcecao se todos os modelos bloquearem a resposta
     * @throws LeituraExcecao para as restantes falhas definitivas
     */
    LeituraResultado ler(Pagina pagina) throws LeituraExcecao;
}
