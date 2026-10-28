package com.ravunana.hinario.leitura;

/**
 * Falha de uma chamada à API do Gemini, com o tipo que o {@link ModelPool}
 * usa para decidir a rotação (PRD secção 5.6 e 5.8).
 */
public class GeminiApiExcecao extends Exception {

    /** Classificação da falha, conforme a tabela do PRD 5.6. */
    public enum Tipo {
        /** 429 com limite por minuto: esperar e repetir no mesmo modelo. */
        QUOTA_MINUTO,
        /** 429 com quota diária esgotada: pausar o modelo até ao reinício. */
        QUOTA_DIARIA,
        /** Resposta bloqueada por segurança/similaridade. */
        BLOQUEADO,
        /** 503 ou erro temporário: repetir com espera crescente. */
        TEMPORARIO,
        /** JSON inválido, fora do esquema, com Markdown ou resposta vazia. */
        RESPOSTA_INVALIDA,
        /** Erro definitivo (400, 403, resposta inesperada). */
        ERRO
    }

    private final Tipo tipo;
    private final int status;

    public GeminiApiExcecao(Tipo tipo, int status, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.status = status;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getStatus() {
        return status;
    }
}
