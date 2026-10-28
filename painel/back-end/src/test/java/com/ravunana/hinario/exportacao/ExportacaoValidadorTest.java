package com.ravunana.hinario.exportacao;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportacaoValidadorTest {

    private static ExportacaoValidador validador;

    @BeforeAll
    static void inicializar() {
        Validator bean = Validation.buildDefaultValidatorFactory().getValidator();
        validador = new ExportacaoValidador(bean);
    }

    private static HinarioNovo hinarioValido() {
        BlocoNovo estrofe = BlocoNovo.estrofe(1, List.of(
                LinhaNovo.de("Primeira linha,"),
                LinhaNovo.de("segunda linha!")));
        BlocoNovo coro = BlocoNovo.coro(List.of(LinhaNovo.de("Coro do hino.")));
        return new HinarioNovo("kmb", "kmb", "Hinário Kimbundu", "2026-10-28T00:00:00Z",
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 10L, "Título", "IERA",
                        List.of(estrofe, coro))));
    }

    @Test
    void hinarioCorretoNaoTemErros() {
        ResultadoValidacao resultado = validador.validar(hinarioValido());

        assertTrue(resultado.valido(), () -> "erros inesperados: " + resultado.erros());
        assertTrue(resultado.avisos().isEmpty(), () -> "avisos inesperados: " + resultado.avisos());
    }

    @Test
    void hinoSemBlocosEErro() {
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of())));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertFalse(resultado.valido());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("sem blocos")));
    }

    @Test
    void estrofeSemNumeroEErro() {
        BlocoNovo estrofe = new BlocoNovo("estrofe", null, List.of(LinhaNovo.de("texto")));
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of(estrofe))));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertFalse(resultado.valido());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("estrofe sem número")));
    }

    @Test
    void coroComNumeroEErro() {
        BlocoNovo coro = new BlocoNovo("coro", 1, List.of(LinhaNovo.de("coro")));
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of(coro))));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertFalse(resultado.valido());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("não pode ter número")));
    }

    @Test
    void recuoForaDoIntervaloEErro() {
        BlocoNovo estrofe = BlocoNovo.estrofe(1, List.of(new LinhaNovo("texto", 3)));
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of(estrofe))));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertFalse(resultado.valido());
    }

    @Test
    void textoForaDeNfcEErro() {
        String decomposto = "e\u0301"; // 'é' partido em e + acento agudo
        BlocoNovo estrofe = BlocoNovo.estrofe(1, List.of(LinhaNovo.de(decomposto)));
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of(estrofe))));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertFalse(resultado.valido());
        assertTrue(resultado.erros().stream().anyMatch(e -> e.contains("NFC")));
    }

    @Test
    void linhaComEspacosExtraEAvisoNaoBloqueia() {
        BlocoNovo estrofe = BlocoNovo.estrofe(1, List.of(LinhaNovo.de("  começo e fim  ")));
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of(estrofe))));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertTrue(resultado.valido());
        assertTrue(resultado.avisos().stream().anyMatch(a -> a.contains("espaços a mais")));
    }

    @Test
    void sequenciaDeEstrofesComBuracosEAviso() {
        BlocoNovo uma = BlocoNovo.estrofe(1, List.of(LinhaNovo.de("a")));
        BlocoNovo tres = BlocoNovo.estrofe(3, List.of(LinhaNovo.de("b")));
        HinarioNovo hinario = new HinarioNovo("kmb", "kmb", "Nome", null,
                List.of(new HinoNovo(java.util.UUID.randomUUID(), 1L, 1L, "Título", null, List.of(uma, tres))));

        ResultadoValidacao resultado = validador.validar(hinario);

        assertTrue(resultado.valido());
        assertTrue(resultado.avisos().stream().anyMatch(a -> a.contains("buracos")));
    }

    @Test
    void caractereForaDoAlfabetoESinalizado() {
        assertTrue(ExportacaoValidador.caracteresInesperados("abcABC012 ’-").isEmpty());
        assertFalse(ExportacaoValidador.caracteresInesperados("мир").isEmpty());
        assertFalse(ExportacaoValidador.caracteresInesperados("emoji 😀").isEmpty());
    }
}
