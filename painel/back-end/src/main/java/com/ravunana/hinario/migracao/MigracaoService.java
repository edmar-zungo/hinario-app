package com.ravunana.hinario.migracao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.exportacao.BlocoNovo;
import com.ravunana.hinario.exportacao.ExportacaoValidador;
import com.ravunana.hinario.exportacao.HinarioNovo;
import com.ravunana.hinario.exportacao.HinoNovo;
import com.ravunana.hinario.exportacao.LinhaNovo;
import com.ravunana.hinario.exportacao.ResultadoValidacao;
import com.ravunana.hinario.model.Estrofe;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.services.DataFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Migração dos dados actuais para o esquema novo (PRD Fase 1):
 * {@code hinario.json} + {@code estrofes.json} ⇒ {@code export/hinario-*.json}
 * e {@code relatorios/migracao.md}.
 *
 * Regra de ouro da migração: **nenhum texto é alterado**. As estrofes são
 * apenas cortadas nas quebras de linha impressas; qualquer diferença de
 * contagem de caracteres é erro.
 */
@Service
public class MigracaoService {

    private static final Logger log = LoggerFactory.getLogger(MigracaoService.class);

    private final DataFiles dataFiles;
    private final ExportacaoValidador validador;
    private final ObjectMapper mapper = new ObjectMapper();

    public MigracaoService(DataFiles dataFiles, ExportacaoValidador validador) {
        this.dataFiles = dataFiles;
        this.validador = validador;
    }

    public ResultadoMigracao migrar() throws IOException {
        Instant geradoEm = Instant.now();

        List<Hino> hinos = ler("hinario.json", new TypeReference<List<Hino>>() {
        });
        List<Estrofe> estrofes = ler("estrofes.json", new TypeReference<List<Estrofe>>() {
        });
        log.info("Migração: {} hinos e {} estrofes lidos de {}", hinos.size(), estrofes.size(), dataFiles.raiz());

        Map<UUID, List<Estrofe>> estrofesPorHino = DiagnosticosMigracao.agruparPorHino(estrofes);

        Map<Linguas, List<Hino>> porLingua = agruparPorLingua(hinos);
        Set<UUID> idsExportados = porLingua.values().stream().flatMap(List::stream)
                .map(Hino::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        Path pastaExport = dataFiles.criarPasta("export");

        List<MigracaoRelatorio.ResumoLingua> resumos = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        List<String> ficheirosGerados = new ArrayList<>();
        long caracteresAntes = 0;
        long caracteresDepois = 0;
        int avisosTotal = 0;

        for (Map.Entry<Linguas, List<Hino>> entrada : porLingua.entrySet()) {
            Linguas lingua = entrada.getKey();
            List<Hino> daLingua = entrada.getValue();
            if (daLingua.isEmpty()) {
                continue;
            }

            HinarioNovo hinario = montar(lingua, daLingua, estrofesPorHino, geradoEm);
            ResultadoValidacao resultado = validador.validar(hinario);
            avisosTotal += resultado.avisos().size();

            String nomeFicheiro = "hinario-" + lingua.getCodigo() + ".json";
            if (!resultado.valido()) {
                resultado.erros().forEach(e -> erros.add(nomeFicheiro + ": " + e));
                resumos.add(new MigracaoRelatorio.ResumoLingua(lingua.getCodigo(), lingua.getNome(),
                        hinario.hinos().size(), contarBlocos(hinario, "estrofe"), contarBlocos(hinario, "coro"),
                        nomeFicheiro, resultado.erros().size(), resultado.avisos().size()));
                continue;
            }

            long antes = contarNaOrigem(daLingua, estrofesPorHino);
            long depois = contarNoEsquemaNovo(hinario);
            caracteresAntes += antes;
            caracteresDepois += depois;

            Path alvo = pastaExport.resolve(nomeFicheiro);
            dataFiles.escreverJson(alvo, hinario);
            ficheirosGerados.add(nomeFicheiro);
            log.info("Gerado {} ({} hinos, {} estrofes, {} coros, {} avisos)",
                    nomeFicheiro, hinario.hinos().size(), contarBlocos(hinario, "estrofe"),
                    contarBlocos(hinario, "coro"), resultado.avisos().size());

            resumos.add(new MigracaoRelatorio.ResumoLingua(lingua.getCodigo(), lingua.getNome(),
                    hinario.hinos().size(), contarBlocos(hinario, "estrofe"), contarBlocos(hinario, "coro"),
                    nomeFicheiro, 0, resultado.avisos().size()));
        }

        if (caracteresAntes != caracteresDepois) {
            erros.add("o texto mudou na migração: " + caracteresAntes + " caracteres antes, "
                    + caracteresDepois + " depois (diferença " + (caracteresDepois - caracteresAntes) + ")");
        }

        List<SecaoRelatorio> secoes = DiagnosticosMigracao.calcular(hinos, estrofes, idsExportados);
        int naoNfc = (int) estrofes.stream()
                .filter(e -> e.getDescricao() != null
                        && !Normalizer.isNormalized(e.getDescricao(), Normalizer.Form.NFC))
                .count();
        int orfas = secoes.stream().filter(s -> s.titulo().equals("Estrofes órfãs"))
                .findFirst().map(SecaoRelatorio::encontrados).orElse(0);

        MigracaoRelatorio relatorio = new MigracaoRelatorio(geradoEm, hinos.size(), estrofes.size(),
                caracteresAntes, caracteresDepois, naoNfc, resumos, erros, secoes);
        Path caminhoRelatorio = dataFiles.subpasta("relatorios").resolve("migracao.md");
        dataFiles.escreverTexto(caminhoRelatorio, relatorio.markdown());
        log.info("Relatório escrito em {}", caminhoRelatorio);

        return new ResultadoMigracao(geradoEm, caminhoRelatorio, ficheirosGerados, erros,
                caracteresAntes, caracteresDepois, orfas, avisosTotal);
    }

    // ------------------------------------------------------------------ montagem

    private HinarioNovo montar(Linguas lingua, List<Hino> hinos, Map<UUID, List<Estrofe>> porHino, Instant geradoEm) {
        List<HinoNovo> hinosNovos = hinos.stream()
                .map(hino -> new HinoNovo(
                        hino.getId(),
                        hino.getNumero(),
                        hino.getPagina(),
                        hino.getTitulo(),
                        hino.getAutor(),
                        estrofesDe(hino, porHino)))
                .toList();
        return new HinarioNovo(lingua.getCodigo(), lingua.getCodigo(), lingua.getNome(),
                geradoEm.toString(), hinosNovos);
    }

    /** Estrofes na ordem original do ficheiro; coro vira bloco {@code coro}. */
    private List<BlocoNovo> estrofesDe(Hino hino, Map<UUID, List<Estrofe>> porHino) {
        List<Estrofe> lista = porHino.getOrDefault(hino.getId(), List.of());
        List<BlocoNovo> blocos = new ArrayList<>(lista.size());
        for (Estrofe estrofe : lista) {
            blocos.add(blocoDe(estrofe));
        }
        return blocos;
    }

    private static BlocoNovo blocoDe(Estrofe estrofe) {
        String texto = estrofe.getDescricao() == null ? "" : estrofe.getDescricao();
        List<LinhaNovo> linhas = Arrays.stream(texto.split("\n", -1)).map(LinhaNovo::de).toList();
        return estrofe.isRefrao()
                ? BlocoNovo.coro(linhas)
                : BlocoNovo.estrofe((int) estrofe.getNumero(), linhas);
    }

    // ------------------------------------------------------------------ contagens

    private static long contarNaOrigem(List<Hino> hinos, Map<UUID, List<Estrofe>> porHino) {
        long total = 0;
        for (Hino hino : hinos) {
            total += hino.getTitulo() == null ? 0 : hino.getTitulo().length();
            for (Estrofe estrofe : porHino.getOrDefault(hino.getId(), List.of())) {
                total += estrofe.getDescricao() == null ? 0 : estrofe.getDescricao().length();
            }
        }
        return total;
    }

    private static long contarNoEsquemaNovo(HinarioNovo hinario) {
        long total = 0;
        for (HinoNovo hino : hinario.hinos()) {
            total += hino.titulo() == null ? 0 : hino.titulo().length();
            for (BlocoNovo bloco : hino.blocos()) {
                String texto = String.join("\n",
                        bloco.linhas().stream().map(LinhaNovo::texto).toList());
                total += texto.length();
            }
        }
        return total;
    }

    private static int contarBlocos(HinarioNovo hinario, String tipo) {
        return (int) hinario.hinos().stream()
                .flatMap(h -> h.blocos().stream())
                .filter(b -> tipo.equals(b.tipo()))
                .count();
    }

    // ------------------------------------------------------------------ utilitários

    private Map<Linguas, List<Hino>> agruparPorLingua(List<Hino> hinos) {
        Map<Linguas, List<Hino>> porLingua = new LinkedHashMap<>();
        for (Linguas lingua : Linguas.values()) {
            porLingua.put(lingua, new ArrayList<>());
        }
        for (Hino hino : hinos) {
            if (hino.getId() == null || hino.getLingua() == null) {
                continue;
            }
            porLingua.get(hino.getLingua()).add(hino);
        }
        Comparator<Hino> ordemDePagina = Comparator.comparingLong(Hino::getPagina)
                .thenComparingLong(Hino::getNumero)
                .thenComparing(h -> String.valueOf(h.getId()));
        porLingua.values().forEach(lista -> lista.sort(ordemDePagina));
        return porLingua;
    }

    private <T> List<T> ler(String nome, TypeReference<List<T>> tipo) throws IOException {
        Path caminho = dataFiles.resolve(nome);
        if (!Files.exists(caminho)) {
            throw new IOException("Ficheiro de dados em falta: " + caminho
                    + " — copie a seed (ver README) ou defina hinario.dados.dir");
        }
        if (Files.size(caminho) == 0) {
            return List.of();
        }
        return mapper.readValue(caminho.toFile(), tipo);
    }
}
