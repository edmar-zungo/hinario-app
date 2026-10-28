package com.ravunana.hinario.migracao;

import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.exportacao.ExportacaoValidador;
import com.ravunana.hinario.model.Estrofe;
import com.ravunana.hinario.model.Hino;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Diagnósticos dos dados actuais (PRD Fase 1 — relatório {@code migracao.md}).
 *
 * Só lê: nunca altera texto nem dados.
 */
final class DiagnosticosMigracao {

    private DiagnosticosMigracao() {
    }

    static List<SecaoRelatorio> calcular(List<Hino> hinos, List<Estrofe> estrofes, Set<UUID> idsExportados) {
        List<SecaoRelatorio> secoes = new ArrayList<>();
        secoes.add(orfas(hinos, estrofes));
        secoes.add(hinosSemEstrofes(hinos, estrofes, idsExportados));
        secoes.add(numeracaoDeHinos(hinos, idsExportados));
        secoes.add(sequenciaDeEstrofes(hinos, estrofes, idsExportados));
        secoes.add(linhasLongas(hinos, estrofes, idsExportados));
        secoes.add(espacosExtra(hinos, estrofes, idsExportados));
        secoes.add(copiasDivergentes(hinos, estrofes, idsExportados));
        return secoes;
    }

    // ------------------------------------------------------------------ órfãs

    private static SecaoRelatorio orfas(List<Hino> hinos, List<Estrofe> estrofes) {
        Set<UUID> existentes = hinos.stream().map(Hino::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> linhas = new ArrayList<>();
        for (Estrofe estrofe : estrofes) {
            Hino hino = estrofe.getHino();
            if (hino != null && existentes.contains(hino.getId())) {
                continue;
            }
            String origem = hino == null
                    ? "sem hino associado"
                    : "do hino nº " + hino.getNumero() + " (" + nomeLingua(hino.getLingua()) + "), que não existe em hinario.json";
            linhas.add("- estrofe nº " + estrofe.getNumero() + " " + origem
                    + " — \"" + trecho(estrofe.getDescricao(), 60) + "\"");
        }
        return new SecaoRelatorio("Estrofes órfãs", linhas.size(), linhas);
    }

    private static SecaoRelatorio hinosSemEstrofes(List<Hino> hinos, List<Estrofe> estrofes, Set<UUID> idsExportados) {
        Set<UUID> comEstrofes = estrofes.stream().map(Estrofe::getHino).filter(Objects::nonNull)
                .map(Hino::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> linhas = new ArrayList<>();
        for (Hino hino : hinos) {
            if (!idsExportados.contains(hino.getId()) || comEstrofes.contains(hino.getId())) {
                continue;
            }
            linhas.add("- nº " + hino.getNumero() + " (" + nomeLingua(hino.getLingua()) + ", pág. "
                    + hino.getPagina() + ") \"" + hino.getTitulo() + "\"");
        }
        return new SecaoRelatorio("Hinos sem estrofes", linhas.size(), linhas);
    }

    // ------------------------------------------------------------- numeração

    private static SecaoRelatorio numeracaoDeHinos(List<Hino> hinos, Set<UUID> idsExportados) {
        Map<Linguas, List<Hino>> porLingua = new LinkedHashMap<>();
        for (Linguas lingua : Linguas.values()) {
            porLingua.put(lingua, new ArrayList<>());
        }
        for (Hino hino : hinos) {
            if (idsExportados.contains(hino.getId()) && hino.getLingua() != null) {
                porLingua.get(hino.getLingua()).add(hino);
            }
        }

        List<String> linhas = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Linguas, List<Hino>> entrada : porLingua.entrySet()) {
            List<Hino> lista = entrada.getValue();
            if (lista.isEmpty()) {
                continue;
            }
            Linguas lingua = entrada.getKey();
            int totalHinos = lista.size();

            Map<Long, List<Hino>> porNumero = lista.stream()
                    .collect(Collectors.groupingBy(Hino::getNumero, LinkedHashMap::new, Collectors.toList()));

            List<Long> numeros = new ArrayList<>(porNumero.keySet());
            long minimo = numeros.stream().mapToLong(Long::longValue).min().orElse(0);
            long maximo = numeros.stream().mapToLong(Long::longValue).max().orElse(0);

            Map<Long, List<Hino>> duplicados = porNumero.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (a, b) -> a, LinkedHashMap::new));

            Set<Long> presentes = new TreeSet<>(porNumero.keySet());
            List<Long> buracos = new ArrayList<>();
            for (long n = minimo; n <= maximo; n++) {
                if (!presentes.contains(n)) {
                    buracos.add(n);
                }
            }

            List<Long> foraDoIntervalo = numeros.stream()
                    .filter(n -> n < 1 || n > totalHinos)
                    .sorted()
                    .toList();

            total += duplicados.size() + buracos.size() + foraDoIntervalo.size();

            linhas.add("- **" + lingua.name() + " (" + lingua.getCodigo() + ")**: " + totalHinos
                    + " hinos, números " + minimo + ".." + maximo);
            linhas.add("  - duplicados: "
                    + (duplicados.isEmpty() ? "nenhum"
                            : duplicados.entrySet().stream()
                                    .map(e -> e.getKey() + " (" + e.getValue().size() + "×) — "
                                            + e.getValue().stream().map(Hino::getTitulo).distinct()
                                                    .map(t -> "\"" + t + "\"").collect(Collectors.joining(" e ")))
                                    .collect(Collectors.joining("; "))));
            linhas.add("  - buracos (" + buracos.size() + "): "
                    + (buracos.isEmpty() ? "nenhum" : intervalosCompactos(buracos)));
            linhas.add("  - fora do intervalo [1.." + totalHinos + "] (" + foraDoIntervalo.size() + "): "
                    + (foraDoIntervalo.isEmpty() ? "nenhum"
                            : intervalosCompactos(foraDoIntervalo)
                                    + " — possíveis números de página em vez de número de hino"));
        }
        return new SecaoRelatorio("Numeração de hinos", total, linhas);
    }

    // --------------------------------------------------------- estrofes 1..N

    private static SecaoRelatorio sequenciaDeEstrofes(List<Hino> hinos, List<Estrofe> estrofes, Set<UUID> idsExportados) {
        Map<UUID, List<Estrofe>> porHino = agruparPorHino(estrofes);

        List<String> linhas = new ArrayList<>();
        for (Hino hino : hinos) {
            if (!idsExportados.contains(hino.getId())) {
                continue;
            }
            List<Integer> numeros = porHino.getOrDefault(hino.getId(), List.of()).stream()
                    .filter(e -> !e.isRefrao())
                    .map(e -> (int) e.getNumero())
                    .toList();
            if (numeros.isEmpty()) {
                continue;
            }

            Set<Integer> distintos = new TreeSet<>(numeros);
            List<Integer> faltam = new ArrayList<>();
            int maximo = distintos.stream().mapToInt(Integer::intValue).max().orElse(0);
            for (int n = 1; n <= maximo; n++) {
                if (!distintos.contains(n)) {
                    faltam.add(n);
                }
            }
            List<Integer> foraDoIntervalo = distintos.stream().filter(n -> n < 1).sorted().toList();
            List<Integer> repetidos = distintos.stream().filter(n -> Collections.frequency(numeros, n) > 1).sorted().toList();
            boolean foraDeOrdem = !numeros.equals(numeros.stream().sorted().toList());

            if (faltam.isEmpty() && repetidos.isEmpty() && foraDoIntervalo.isEmpty() && !foraDeOrdem) {
                continue;
            }

            List<String> problemas = new ArrayList<>();
            if (!foraDoIntervalo.isEmpty()) {
                problemas.add("número fora do intervalo " + foraDoIntervalo);
            }
            if (!faltam.isEmpty()) {
                problemas.add("buracos: faltam " + faltam);
            }
            if (!repetidos.isEmpty()) {
                problemas.add("repetidos: " + repetidos);
            }
            if (foraDeOrdem) {
                problemas.add("ordem no ficheiro não é crescente " + numeros);
            }

            linhas.add("- nº " + hino.getNumero() + " (" + nomeLingua(hino.getLingua()) + ") \""
                    + hino.getTitulo() + "\": " + String.join("; ", problemas));
        }
        return new SecaoRelatorio("Sequência de estrofes por hino", linhas.size(), linhas);
    }

    // ------------------------------------------------------------ diagnósticos

    private static SecaoRelatorio linhasLongas(List<Hino> hinos, List<Estrofe> estrofes, Set<UUID> idsExportados) {
        Map<UUID, Hino> indice = indiceDeHinos(hinos);
        List<String> linhas = new ArrayList<>();
        for (Estrofe estrofe : estrofes) {
            String descricao = estrofe.getDescricao();
            if (descricao == null || descricao.contains("\n") || descricao.length() <= 80) {
                continue;
            }
            Hino hino = estrofe.getHino();
            if (hino == null || !idsExportados.contains(hino.getId())) {
                continue;
            }
            linhas.add("- \"" + titulo(hino, indice) + "\" (" + nomeLingua(hino.getLingua())
                    + ", estrofe " + estrofe.getNumero() + "): " + descricao.length()
                    + " caracteres sem quebra de linha — \"" + trecho(descricao, 70) + "\"");
        }
        return new SecaoRelatorio("Estrofes sem quebra de linha com mais de 80 caracteres", linhas.size(), linhas);
    }

    private static SecaoRelatorio espacosExtra(List<Hino> hinos, List<Estrofe> estrofes, Set<UUID> idsExportados) {
        Map<UUID, Hino> indice = indiceDeHinos(hinos);
        List<String> linhas = new ArrayList<>();
        for (Estrofe estrofe : estrofes) {
            Hino hino = estrofe.getHino();
            if (hino == null || !idsExportados.contains(hino.getId())) {
                continue;
            }
            String descricao = estrofe.getDescricao();
            if (descricao == null) {
                continue;
            }
            List<String> problemas = new ArrayList<>();
            String[] linhasImpressas = descricao.split("\n", -1);
            for (int i = 0; i < linhasImpressas.length; i++) {
                String linha = linhasImpressas[i];
                if (linha.isEmpty()) {
                    problemas.add("linha " + (i + 1) + " em branco");
                } else if (!linha.equals(linha.strip())) {
                    problemas.add("linha " + (i + 1) + " com espaços no início/fim");
                } else if (linha.contains("  ")) {
                    problemas.add("linha " + (i + 1) + " com espaço duplo");
                } else if (ExportacaoValidador.temEspacosExtra(linha)) {
                    problemas.add("linha " + (i + 1) + " com tabulação");
                }
            }
            if (problemas.isEmpty()) {
                continue;
            }
            linhas.add("- \"" + titulo(hino, indice) + "\" (" + nomeLingua(hino.getLingua())
                    + ", estrofe " + estrofe.getNumero() + "): " + String.join("; ", problemas));
        }
        return new SecaoRelatorio("Espaços a mais e linhas em branco", linhas.size(), linhas);
    }

    private static SecaoRelatorio copiasDivergentes(List<Hino> hinos, List<Estrofe> estrofes, Set<UUID> idsExportados) {
        Map<UUID, List<Estrofe>> porHino = agruparPorHino(estrofes);

        Map<String, List<Hino>> porTitulo = new LinkedHashMap<>();
        for (Hino hino : hinos) {
            if (!idsExportados.contains(hino.getId()) || hino.getLingua() == null) {
                continue;
            }
            porTitulo.computeIfAbsent(hino.getLingua().name() + "|" + chaveTitulo(hino.getTitulo()),
                    k -> new ArrayList<>()).add(hino);
        }

        List<String> linhas = new ArrayList<>();
        for (List<Hino> grupo : porTitulo.values()) {
            if (grupo.size() < 2) {
                continue;
            }
            grupo.sort(Comparator.comparingLong(Hino::getNumero));
            Hino primeiro = grupo.get(0);
            for (int i = 1; i < grupo.size(); i++) {
                Hino outro = grupo.get(i);
                List<String> a = textosDeHino(primeiro, porHino);
                List<String> b = textosDeHino(outro, porHino);
                int comparadas = Math.min(a.size(), b.size());
                int diferentes = 0;
                for (int j = 0; j < comparadas; j++) {
                    if (!Objects.equals(a.get(j), b.get(j))) {
                        diferentes++;
                    }
                }
                boolean divergente = diferentes > 0 || a.size() != b.size();
                linhas.add("- \"" + primeiro.getTitulo() + "\" ("
                        + nomeLingua(primeiro.getLingua()) + "): hino " + primeiro.getNumero()
                        + " e hino " + outro.getNumero() + " — "
                        + (divergente
                                ? "textos diferentes (" + diferentes + " de " + comparadas
                                        + " estrofes divergem" + (a.size() != b.size() ? ", contagens " + a.size() + "/" + b.size() : "") + ")"
                                : "textos idênticos"));
            }
        }
        return new SecaoRelatorio("Cópias de hino divergentes", linhas.size(), linhas);
    }

    // ----------------------------------------------------------------- utilitários

    static Map<UUID, List<Estrofe>> agruparPorHino(List<Estrofe> estrofes) {
        Map<UUID, List<Estrofe>> porHino = new LinkedHashMap<>();
        for (Estrofe estrofe : estrofes) {
            if (estrofe.getHino() == null || estrofe.getHino().getId() == null) {
                continue;
            }
            porHino.computeIfAbsent(estrofe.getHino().getId(), k -> new ArrayList<>()).add(estrofe);
        }
        return porHino;
    }

    private static Map<UUID, Hino> indiceDeHinos(List<Hino> hinos) {
        return hinos.stream()
                .collect(Collectors.toMap(Hino::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static List<String> textosDeHino(Hino hino, Map<UUID, List<Estrofe>> porHino) {
        return porHino.getOrDefault(hino.getId(), List.of()).stream()
                .map(Estrofe::getDescricao)
                .toList();
    }

    private static String titulo(Hino hino, Map<UUID, Hino> indice) {
        Hino completo = indice.getOrDefault(hino.getId(), hino);
        return completo.getTitulo() == null ? "" : completo.getTitulo();
    }

    private static String nomeLingua(Linguas lingua) {
        return lingua == null ? "sem língua" : lingua.getCodigo();
    }

    private static String chaveTitulo(String titulo) {
        if (titulo == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(titulo, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String trecho(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        String umaLinha = texto.replace('\n', ' ').trim();
        return umaLinha.length() <= maximo ? umaLinha : umaLinha.substring(0, maximo - 3) + "...";
    }

    /** Converte [1,2,3,7,9] em "1-3, 7, 9". */
    static String intervalosCompactos(List<Long> valores) {
        if (valores.isEmpty()) {
            return "nenhum";
        }
        List<Long> ordenados = valores.stream().sorted().toList();
        StringBuilder saida = new StringBuilder();
        long inicio = ordenados.get(0);
        long anterior = inicio;
        for (int i = 1; i <= ordenados.size(); i++) {
            long actual = i < ordenados.size() ? ordenados.get(i) : anterior + 2;
            if (actual == anterior + 1) {
                anterior = actual;
                continue;
            }
            if (saida.length() > 0) {
                saida.append(", ");
            }
            saida.append(inicio == anterior ? String.valueOf(inicio) : inicio + "-" + anterior);
            if (i < ordenados.size()) {
                inicio = actual;
                anterior = actual;
            }
        }
        return saida.toString();
    }
}
