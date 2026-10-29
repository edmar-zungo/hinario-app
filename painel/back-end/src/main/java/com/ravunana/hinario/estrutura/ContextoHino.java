package com.ravunana.hinario.estrutura;

/**
 * Hino aberto no fim de uma página, para validar a ligação da página seguinte
 * (PRD 5.7.4): a página que abre na estrofe N só se junta se a anterior
 * terminou na N−1; caso contrário, pede-se confirmação.
 */
public record ContextoHino(Integer numeroHino, String titulo, Integer ultimaEstrofe) {
}
