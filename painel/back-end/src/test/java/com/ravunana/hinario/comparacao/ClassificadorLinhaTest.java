package com.ravunana.hinario.comparacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** PRD 5.7.3: igual, so_acento, so_pontuacao, divergente — nunca altera texto. */
class ClassificadorLinhaTest {

    private final ClassificadorLinha classificador = new ClassificadorLinha();

    @Test
    void linhasIdenticasSaoIguais() {
        assertEquals("igual", classificador.classificar("M’ uhukunii aJesus", "M’ uhukunii aJesus"));
    }

    @Test
    void diferencaDeAcentoÉSoAcento() {
        assertEquals("so_acento",
                classificador.classificar("Sai-ku akua diniota", "Sai-ku akua d\u00edn\u00edota"));
    }

    @Test
    void diferencaDePontuacaoÉSoPontuacao() {
        assertEquals("so_pontuacao",
                classificador.classificar("Sai-ku akua diniota", "Sai-ku akua diniota."));
    }

    @Test
    void apostrofoRetoECurvoContamComoPontuacao() {
        assertEquals("so_pontuacao", classificador.classificar("M' ukia", "M’ ukia"));
    }

    @Test
    void palavrasDiferentesSaoDivergentes() {
        assertEquals("divergente", classificador.classificar("kuanza kwetu", "kuanza kwenu"));
    }

    @Test
    void nfcENfdSaoIguais() {
        // "caf\u00e9" em NFC (\u00e9 precomposto) vs "cafe\u0301" em NFD
        assertEquals("igual", classificador.classificar("caf\u00e9", "cafe\u0301"));
    }

    @Test
    void espacosAMaisNasExtremidadesIgnoramSe() {
        assertEquals("igual", classificador.classificar("  kuanza ", "kuanza"));
    }

    @Test
    void textoSoDeUmLadoNaoExplode() {
        assertEquals("so_pontuacao", classificador.classificar("kuanza.", "kuanza"));
        assertEquals("divergente", classificador.classificar("", "kuanza"));
    }
}
