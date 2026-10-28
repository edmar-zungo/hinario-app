package com.ravunana.hinario.exportacao;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validação do esquema novo: Bean Validation (estrutura) + verificações
 * próprias (PRD secção 5.7 e Fase 1).
 *
 * As verificações próprias nunca alteram o texto: só acrescentam erros ou
 * avisos.
 */
@Component
public class ExportacaoValidador {

    private static final Pattern ID = Pattern.compile("[a-z0-9]{2,10}");

    private final Validator beanValidator;

    public ExportacaoValidador(Validator beanValidator) {
        this.beanValidator = beanValidator;
    }

    public ResultadoValidacao validar(HinarioNovo hinario) {
        List<String> erros = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        if (hinario == null) {
            return new ResultadoValidacao(List.of("hinário nulo"), List.of());
        }

        for (ConstraintViolation<HinarioNovo> violacao : beanValidator.validate(hinario)) {
            erros.add("estrutura: " + caminho(violacao) + " — " + violacao.getMessage());
        }

        if (hinario.id() != null && !ID.matcher(hinario.id()).matches()) {
            erros.add("id inválido (minúsculas e sem espaços): " + hinario.id());
        }
        if (hinario.lingua() != null && !ID.matcher(hinario.lingua()).matches()) {
            erros.add("lingua inválida (código minúsculo): " + hinario.lingua());
        }
        if (hinario.hinos() == null || hinario.hinos().isEmpty()) {
            erros.add("o hinário não tem hinos");
            return new ResultadoValidacao(erros, avisos);
        }

        verificarOrdenacao(hinario, avisos);
        verificarNumerosDeHino(hinario, avisos);

        for (HinoNovo hino : hinario.hinos()) {
            if (hino == null) {
                erros.add("hino nulo na lista");
                continue;
            }
            String onde = "hino " + hino.numero() + " (pág. " + hino.pagina() + ")";

            if (hino.titulo() != null && temEspacosExtra(hino.titulo())) {
                avisos.add(onde + ": título com espaços a mais — \"" + hino.titulo() + "\"");
            }

            if (hino.blocos() == null || hino.blocos().isEmpty()) {
                erros.add(onde + ": hino sem blocos");
                continue;
            }

            List<Integer> numerosDeEstrofe = new ArrayList<>();
            for (BlocoNovo bloco : hino.blocos()) {
                if (bloco == null) {
                    erros.add(onde + ": bloco nulo");
                    continue;
                }
                validarBloco(onde, bloco, erros, avisos, numerosDeEstrofe);
            }
            verificarSequenciaDeEstrofes(onde, numerosDeEstrofe, avisos);
        }

        return new ResultadoValidacao(erros, avisos);
    }

    private void verificarOrdenacao(HinarioNovo hinario, List<String> avisos) {
        String anterior = null;
        for (HinoNovo hino : hinario.hinos()) {
            if (hino == null || hino.pagina() == null || hino.numero() == null) {
                continue;
            }
            String atual = String.format("%08d-%08d", hino.pagina(), hino.numero());
            if (anterior != null && atual.compareTo(anterior) < 0) {
                avisos.add("hino " + hino.numero() + " (pág. " + hino.pagina()
                        + ") aparece fora de ordem de página/número");
            }
            anterior = atual;
        }
    }

    private void verificarNumerosDeHino(HinarioNovo hinario, List<String> avisos) {
        Set<String> vistos = new LinkedHashSet<>();
        for (HinoNovo hino : hinario.hinos()) {
            if (hino == null || hino.numero() == null) {
                continue;
            }
            if (!vistos.add(String.valueOf(hino.numero()))) {
                avisos.add("número de hino repetido no hinário: " + hino.numero());
            }
        }
    }

    private void validarBloco(String onde, BlocoNovo bloco, List<String> erros, List<String> avisos,
                              List<Integer> numerosDeEstrofe) {
        if ("estrofe".equals(bloco.tipo())) {
            if (bloco.numero() == null) {
                erros.add(onde + ": estrofe sem número");
            } else {
                if (bloco.numero() < 1) {
                    avisos.add(onde + ": número de estrofe fora do intervalo (" + bloco.numero() + ")");
                }
                if (!numerosDeEstrofe.add(bloco.numero())) {
                    avisos.add(onde + ": número de estrofe repetido (" + bloco.numero() + ")");
                }
            }
        } else if (bloco.numero() != null) {
            erros.add(onde + ": bloco '" + bloco.tipo() + "' não pode ter número de estrofe");
        }

        if (bloco.linhas() == null || bloco.linhas().isEmpty()) {
            erros.add(onde + ": bloco '" + bloco.tipo() + "' sem linhas");
            return;
        }

        for (int i = 0; i < bloco.linhas().size(); i++) {
            LinhaNovo linha = bloco.linhas().get(i);
            if (linha == null || linha.texto() == null) {
                erros.add(onde + ": linha " + i + " sem texto");
                continue;
            }
            validarLinha(onde, i, linha, erros, avisos);
        }
    }

    private void validarLinha(String onde, int indice, LinhaNovo linha, List<String> erros, List<String> avisos) {
        String texto = linha.texto();

        if (!Normalizer.isNormalized(texto, Normalizer.Form.NFC)) {
            erros.add(onde + ": linha " + indice + " não está em Unicode NFC");
        }

        if (texto.isEmpty()) {
            avisos.add(onde + ": linha " + indice + " vazia (linha impressa em branco)");
        } else if (temEspacosExtra(texto)) {
            avisos.add(onde + ": linha " + indice + " com espaços a mais — \"" + limitar(texto) + "\"");
        }

        for (char inesperado : caracteresInesperados(texto)) {
            avisos.add(onde + ": linha " + indice + " contém o carácter '"
                    + inesperado + "' (U+" + String.format("%04X", (int) inesperado) + ") fora do alfabeto esperado");
        }
    }

    private void verificarSequenciaDeEstrofes(String onde, List<Integer> numeros, List<String> avisos) {
        if (numeros.size() < 2) {
            return;
        }
        int maximo = numeros.stream().mapToInt(Integer::intValue).max().orElse(0);
        Set<Integer> vistos = new LinkedHashSet<>(numeros);
        List<Integer> faltam = new ArrayList<>();
        for (int n = 1; n <= maximo; n++) {
            if (!vistos.contains(n)) {
                faltam.add(n);
            }
        }
        if (!faltam.isEmpty()) {
            avisos.add(onde + ": sequência de estrofes com buracos (faltam " + faltam + ")");
        }
    }

    /** Espaços duplos, espaços no início/fim ou tabulações. */
    public static boolean temEspacosExtra(String texto) {
        if (texto == null) {
            return false;
        }
        if (!texto.equals(texto.strip())) {
            return true;
        }
        return texto.contains("  ") || texto.contains("\t");
    }

    /**
     * Conjunto esperado (PRD 5.7.2): letras latinas com diacríticos, dígitos,
     * pontuação comum, apóstrofo tipográfico e hífen. Devolve só os caracteres
     * fora do conjunto — nada é removido.
     */
    static Set<Character> caracteresInesperados(String texto) {
        Set<Character> resultado = new LinkedHashSet<>();
        if (texto == null) {
            return resultado;
        }
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (foraDoAlfabeto(c)) {
                resultado.add(c);
            }
        }
        return resultado;
    }

    /** {@code true} quando o carácter está fora do conjunto esperado. */
    private static boolean foraDoAlfabeto(char c) {
        int tipo = Character.getType(c);
        boolean marca = tipo == Character.NON_SPACING_MARK || tipo == Character.COMBINING_SPACING_MARK
                || tipo == Character.ENCLOSING_MARK || tipo == Character.MODIFIER_LETTER;
        if (marca) {
            return false;
        }
        if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
            return false;
        }
        if (c < 0x80) {
            return c < 0x20 || c == 0x7F; // controlos e DEL
        }
        // Latim: Latin-1 Supplement, Latin Extended-A/B e adicionais
        if (c >= 0x00A0 && c <= 0x024F) {
            return false;
        }
        // Pontuação tipográfica comum (inclui ’ “ ” – — …)
        if (c >= 0x2000 && c <= 0x206F) {
            return false;
        }
        return true;
    }

    private static String caminho(ConstraintViolation<?> violacao) {
        return violacao.getPropertyPath() + "";
    }

    private static String limitar(String texto) {
        String umaLinha = texto.replace('\n', ' ');
        return umaLinha.length() <= 60 ? umaLinha : umaLinha.substring(0, 57) + "...";
    }
}
