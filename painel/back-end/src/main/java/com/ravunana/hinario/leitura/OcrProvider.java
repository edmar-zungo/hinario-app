package com.ravunana.hinario.leitura;

import com.ravunana.hinario.pagina.Pagina;

/**
 * Leitor de páginas através do serviço de OCR da Groq.
 */
public interface OcrProvider {

    /** Nome do fornecedor. */
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
