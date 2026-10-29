package com.ravunana.hinario.estrutura;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Estrutura de uma página (PRD Fase 5): os blocos para o ficheiro da página e
 * o hino que fica aberto para a página seguinte.
 */
public record EstruturaPagina(ArrayNode blocos, ContextoHino hinoAberto, boolean continuaDoAnterior) {
}
