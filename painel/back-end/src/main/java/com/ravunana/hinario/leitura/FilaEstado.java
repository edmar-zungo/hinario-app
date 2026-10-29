package com.ravunana.hinario.leitura;

import java.util.List;

/** Estado da fila e das quotas para {@code GET /fila} (PRD secção 7). */
public record FilaEstado(
        String estado,
        int pendentes,
        boolean geminiDisponivel,
        boolean tesseractDisponivel,
        boolean quotaEsgotada,
        String retomaAte,
        List<PaginaFila> paginas,
        List<ModeloFila> modelos) {

    /** Página na fila, com o seu estado em disco. */
    public record PaginaFila(int pagina, String hinarioId, String estado) {
    }

    /** Contadores e pausa de um modelo da cadeia. */
    public record ModeloFila(String modelo, boolean pausado, String pausadoAte,
                             int pedidosMinuto, int pedidosDia) {
    }
}
