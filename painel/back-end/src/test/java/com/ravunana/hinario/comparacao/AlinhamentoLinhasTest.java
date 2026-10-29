package com.ravunana.hinario.comparacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Distância de edição e alinhamento (PRD 5.7.3). */
class AlinhamentoLinhasTest {

    private final AlinhamentoLinhas alinhamento = new AlinhamentoLinhas();

    @Test
    void distanciaEdicaoClassica() {
        assertEquals(0, AlinhamentoLinhas.distanciaEdicao("kuanza", "kuanza"));
        assertEquals(1, AlinhamentoLinhas.distanciaEdicao("kuanza", "kuanzas"));
        assertEquals(1, AlinhamentoLinhas.distanciaEdicao("", "a"));
    }

    @Test
    void linhasIguaisAlinhamUmaAUna() {
        var pares = alinhamento.alinhar(java.util.List.of("a", "b"), java.util.List.of("a", "b"));
        assertEquals(2, pares.size());
        assertEquals(new AlinhamentoLinhas.Par(0, 0), pares.get(0));
        assertEquals(new AlinhamentoLinhas.Par(1, 1), pares.get(1));
    }

    @Test
    void linhaEmFaltaFicaComoOmissao() {
        var pares = alinhamento.alinhar(java.util.List.of("a", "b", "c"), java.util.List.of("a", "c"));
        assertEquals(new AlinhamentoLinhas.Par(0, 0), pares.get(0));
        assertEquals(new AlinhamentoLinhas.Par(1, -1), pares.get(1));
        assertEquals(new AlinhamentoLinhas.Par(2, 1), pares.get(2));
    }

    @Test
    void linhaSoDoTesseractFicaComoOmissaoDoOutroLado() {
        var pares = alinhamento.alinhar(java.util.List.of("a"), java.util.List.of("a", "z"));
        assertEquals(new AlinhamentoLinhas.Par(-1, 1), pares.get(1));
    }

    @Test
    void paresDiferentesNaoSeEmpareLham() {
        var pares = alinhamento.alinhar(
                java.util.List.of("uhukunii aJesus"), java.util.List.of("completamente diferente"));
        assertEquals(2, pares.size());
        assertEquals(new AlinhamentoLinhas.Par(0, -1), pares.get(0));
        assertEquals(new AlinhamentoLinhas.Par(-1, 0), pares.get(1));
    }

    @Test
    void similaridadeÉCrescenteComIgualdade() {
        assertTrue(AlinhamentoLinhas.similaridade("abc", "abc") == 1.0);
        assertTrue(AlinhamentoLinhas.similaridade("abc", "abd") > 0.5);
        assertTrue(AlinhamentoLinhas.similaridade("abc", "xyz") < 0.5);
    }
}
