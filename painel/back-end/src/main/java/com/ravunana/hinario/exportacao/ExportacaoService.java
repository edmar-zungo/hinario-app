package com.ravunana.hinario.exportacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import com.ravunana.hinario.services.HinoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Exportação final (PRD Fase 7): monta os hinos a partir das páginas
 * `confirmada` do painel e grava {@code export/hinario-<id>.json}.
 *
 * Regras:
 * - só entram linhas com {@code revisto: true} (quem reviu confirmou);
 * - estrofe que continua na página seguinte é **juntada** às linhas da
 *   estrofe anterior do mesmo hino (a divisão de página não é de poesia);
 * - o {@code id} (UUID) e o {@code autor} vêm do {@code hinario.json}
 *   actual, para os favoritos continuarem a servir; hino novo recebe UUID
 *   novo e autor "IERA";
 * - {@code bloco_sem_numero} por classificar bloqueia o hino — o humano
 *   decide na revisão se é coro ou estrofe;
 * - validações finais: NFC, sem espaços a mais, numeração contínua de
 *   estrofes, sem números de hino repetidos por língua.
 */
@Service
public class ExportacaoService {

    private static final Logger log = LoggerFactory.getLogger(ExportacaoService.class);

    private final PaginaService paginas;
    private final HinoService hinosAntigos;
    private final ExportacaoValidador validador;
    private final DataFiles dataFiles;

    public ExportacaoService(PaginaService paginas, HinoService hinosAntigos,
                             ExportacaoValidador validador, DataFiles dataFiles) {
        this.paginas = paginas;
        this.hinosAntigos = hinosAntigos;
        this.validador = validador;
        this.dataFiles = dataFiles;
    }

    /** Resultado de exportar um hinário. */
    public record ResultadoExportacao(
            String ficheiro, boolean gerado, int hinos, int estrofes, int coros,
            List<String> erros, List<String> avisos) {
    }

    /**
     * Exporta um hinário (código da língua) ou, sem código, todos os que
     * têm páginas confirmadas.
     */
    public List<ResultadoExportacao> exportar(String codigoLingua) {
        List<String> codigos = new ArrayList<>();
        if (codigoLingua != null && !codigoLingua.isBlank()) {
            try {
                Linguas.porCodigo(codigoLingua);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            codigos.add(codigoLingua.toLowerCase());
        } else {
            for (Linguas lingua : Linguas.values()) {
                codigos.add(lingua.getCodigo());
            }
        }

        List<ResultadoExportacao> resultados = new ArrayList<>();
        for (String codigo : codigos) {
            resultados.add(exportarLingua(codigo));
        }
        return resultados;
    }

    private ResultadoExportacao exportarLingua(String codigo) {
        List<String> erros = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        HinarioNovo hinario;
        try {
            hinario = montar(codigo, erros, avisos);
        } catch (Exception e) {
            return novoResultado(codigo, false, 0, 0, 0,
                    List.of("não foi possível montar o hinário: " + e.getMessage()), List.of());
        }
        if (hinario == null || hinario.hinos().isEmpty()) {
            if (erros.isEmpty()) {
                erros.add("não há páginas confirmadas para o hinário " + codigo);
            }
            return novoResultado(codigo, false, 0, 0, 0, erros, avisos);
        }

        ResultadoValidacao validacao = validador.validar(hinario);
        erros.addAll(validacao.erros());
        // validações finais da Fase 7: certos avisos passam a bloquear
        for (String aviso : validacao.avisos()) {
            if (bloqueiaExportacao(aviso)) {
                erros.add(aviso);
            } else {
                avisos.add(aviso);
            }
        }

        if (!erros.isEmpty()) {
            return novoResultado(codigo, false, hinario.hinos().size(),
                    contar(hinario, "estrofe"), contar(hinario, "coro"), erros, avisos);
        }

        String nome = "hinario-" + codigo + ".json";
        try {
            dataFiles.escreverJson(dataFiles.criarPasta("export").resolve(nome), hinario);
        } catch (Exception e) {
            erros.add("falha ao escrever " + nome + ": " + e.getMessage());
            return novoResultado(codigo, false, hinario.hinos().size(),
                    contar(hinario, "estrofe"), contar(hinario, "coro"), erros, avisos);
        }
        log.info("Exportado {} ({} hinos, {} estrofes, {} coros, {} avisos)",
                nome, hinario.hinos().size(), contar(hinario, "estrofe"),
                contar(hinario, "coro"), avisos.size());
        return novoResultado(nome, true, hinario.hinos().size(),
                contar(hinario, "estrofe"), contar(hinario, "coro"), erros, avisos);
    }

    /** Avisos que, na exportação final, bloqueiam (PRD Fase 7). */
    private static boolean bloqueiaExportacao(String aviso) {
        return aviso.contains("sequência de estrofes com buracos")
                || aviso.contains("número de hino repetido")
                || aviso.contains("com espaços a mais")
                || aviso.contains("número de estrofe repetido");
    }

    /**
     * Hino montado a partir das páginas confirmadas (para
     * {@code GET /comparar/{hinoId}}); vazio quando ainda não há leitura.
     */
    public Optional<HinoNovo> hinoMontado(String codigo, long numeroHino) {
        List<String> erros = new ArrayList<>();
        HinarioNovo hinario;
        try {
            hinario = montar(codigo, erros, new ArrayList<>());
        } catch (Exception e) {
            return Optional.empty();
        }
        if (hinario == null) {
            return Optional.empty();
        }
        return hinario.hinos().stream().filter(h -> h.numero() == numeroHino).findFirst();
    }

    // ------------------------------------------------------------------ montagem

    /**
     * Monta o {@link HinarioNovo} de uma língua a partir das páginas
     * confirmadas. Os erros recebidos pela lista descrevem problemas de
     * montagem (bloco por classificar, etc.).
     */
    HinarioNovo montar(String codigo, List<String> erros, List<String> avisos) throws Exception {
        Linguas lingua = Linguas.porCodigo(codigo);
        List<Pagina> confirmadas = new ArrayList<>();
        for (Pagina pagina : paginas.listar()) {
            if (pagina.estado() == EstadoPagina.CONFIRMADA && codigo.equals(pagina.hinarioId())) {
                confirmadas.add(pagina);
            }
        }
        confirmadas.sort(Comparator.comparingInt(Pagina::pagina));
        if (confirmadas.isEmpty()) {
            return null;
        }

        Map<Long, Hino> antigosPorNumero = hinosAntigosPorNumero(lingua);

        List<HinoNovo> hinosNovos = new ArrayList<>();
        HinoAtual atual = null;
        for (Pagina pagina : confirmadas) {
            JsonNode blocos = pagina.blocos();
            if (blocos == null || !blocos.isArray()) {
                erros.add("página " + pagina.pagina() + ": sem estrutura (blocos)");
                continue;
            }
            for (JsonNode bloco : blocos) {
                String tipo = bloco.path("tipo").asText("");
                switch (tipo) {
                    case "titulo_hino" -> {
                        if (atual != null) {
                            fechar(atual, hinosNovos, erros, avisos, antigosPorNumero);
                        }
                        Integer numero = bloco.path("numeroHino").isInt()
                                ? bloco.path("numeroHino").asInt() : null;
                        String titulo = bloco.path("titulo").asText("");
                        if (numero == null) {
                            erros.add("página " + pagina.pagina() + ": título sem número de hino");
                        }
                        if (titulo.isBlank()) {
                            erros.add("página " + pagina.pagina() + ": hino sem título");
                        }
                        atual = new HinoAtual(numero == null ? 0 : numero, titulo, pagina.pagina());
                    }
                    case "estrofe", "coro" -> {
                        if (atual == null) {
                            Integer numeroHino = bloco.path("numeroHino").isInt()
                                    ? bloco.path("numeroHino").asInt() : null;
                            if (numeroHino == null) {
                                erros.add("página " + pagina.pagina()
                                        + ": bloco antes de qualquer título de hino");
                                continue;
                            }
                            atual = new HinoAtual(numeroHino, null, pagina.pagina());
                        }
                        juntarBloco(atual, bloco, tipo, pagina, erros);
                    }
                    default -> erros.add("página " + pagina.pagina()
                            + ": bloco sem número por classificar (confirme se é coro ou estrofe"
                            + " na revisão) — " + tipo);
                }
            }
        }
        if (atual != null) {
            fechar(atual, hinosNovos, erros, avisos, antigosPorNumero);
        }

        hinosNovos.sort(Comparator.comparingLong(HinoNovo::pagina).thenComparingLong(HinoNovo::numero));
        return new HinarioNovo(codigo, codigo, lingua.getNome(),
                Instant.now().toString(), hinosNovos);
    }

    /** Acrescenta um bloco, juntando a continuação da mesma estrofe. */
    private void juntarBloco(HinoAtual hino, JsonNode bloco, String tipo, Pagina pagina,
                             List<String> erros) {
        List<LinhaNovo> linhas = linhasRevistas(bloco);
        if (linhas.isEmpty()) {
            erros.add("página " + pagina.pagina() + ": bloco '" + tipo
                    + "' sem linhas revistas");
            return;
        }
        boolean continuacao = bloco.path("continuacao").asBoolean(false);
        if ("estrofe".equals(tipo) && continuacao && !hino.blocos.isEmpty()) {
            BlocoAnterior ultimo = hino.blocos.get(hino.blocos.size() - 1);
            Integer numero = bloco.path("numeroEstrofe").isInt()
                    ? bloco.path("numeroEstrofe").asInt() : null;
            if ("estrofe".equals(ultimo.tipo()) && Objects.equals(ultimo.numero(), numero)) {
                List<LinhaNovo> juntas = new ArrayList<>(ultimo.linhas());
                juntas.addAll(linhas);
                hino.blocos.set(hino.blocos.size() - 1,
                        new BlocoAnterior("estrofe", numero, juntas));
                hino.marcarIncompletaFim(false);
                return;
            }
        }
        Integer numero = "estrofe".equals(tipo) && bloco.path("numeroEstrofe").isInt()
                ? bloco.path("numeroEstrofe").asInt() : null;
        hino.blocos.add(new BlocoAnterior(tipo, numero, linhas));
        hino.marcarIncompletaFim(bloco.path("incompleta").asBoolean(false));
    }

    /** Só as linhas confirmadas pelo revisor; a leitura original intocada. */
    private List<LinhaNovo> linhasRevistas(JsonNode bloco) {
        List<LinhaNovo> linhas = new ArrayList<>();
        JsonNode array = bloco.get("linhas");
        if (array == null || !array.isArray()) {
            return linhas;
        }
        for (JsonNode linha : array) {
            if (linha.path("revisto").asBoolean(false)) {
                linhas.add(new LinhaNovo(linha.path("texto").asText(""),
                        linha.path("recuo").asInt(0)));
            }
        }
        return linhas;
    }

    private void fechar(HinoAtual hino, List<HinoNovo> hinos, List<String> erros,
                        List<String> avisos, Map<Long, Hino> antigos) {
        if (hino.blocos.isEmpty()) {
            erros.add("hino " + hino.numero() + " (pág. " + hino.pagina() + "): sem blocos revistos");
            return;
        }
        if (hino.incompletaNoFim()) {
            BlocoAnterior ultimo = hino.blocos.get(hino.blocos.size() - 1);
            erros.add("hino " + hino.numero() + " (pág. " + hino.pagina()
                    + "): estrofe " + ultimo.numero() + " marcada incompleta"
                    + " (confirme a página seguinte)");
            return;
        }
        List<BlocoNovo> blocos = new ArrayList<>();
        for (BlocoAnterior bloco : hino.blocos()) {
            if (bloco.tipo().equals("estrofe")) {
                blocos.add(BlocoNovo.estrofe(bloco.numero() == null ? 0 : bloco.numero(), bloco.linhas()));
            } else {
                blocos.add(BlocoNovo.coro(bloco.linhas()));
            }
        }
        Hino antigo = hino.numero() == 0 ? null : antigos.get((long) hino.numero());
        UUID id = antigo != null && antigo.getId() != null ? antigo.getId() : UUID.randomUUID();
        String autor = antigo != null ? antigo.getAutor() : "IERA";
        String titulo = hino.titulo() != null ? hino.titulo()
                : (antigo != null ? antigo.getTitulo() : "");
        hinos.add(new HinoNovo(id, (long) hino.numero(), (long) hino.pagina(), titulo, autor, blocos));
        if (antigo == null) {
            avisos.add("hino " + hino.numero() + ": novo (não existia no JSON actual); id gerado");
        }
    }

    private Map<Long, Hino> hinosAntigosPorNumero(Linguas lingua) {
        Map<Long, Hino> porNumero = new LinkedHashMap<>();
        try {
            for (Hino hino : hinosAntigos.getAllHinos()) {
                if (hino.getLingua() == lingua) {
                    porNumero.putIfAbsent(hino.getNumero(), hino);
                }
            }
        } catch (Exception e) {
            log.warn("hinario.json indisponível para exportação: {}", e.getMessage());
        }
        return porNumero;
    }

    private static int contar(HinarioNovo hinario, String tipo) {
        String corrigido = tipo.equals("strofe") ? "estrofe" : tipo;
        return (int) hinario.hinos().stream()
                .flatMap(h -> h.blocos().stream())
                .filter(b -> corrigido.equals(b.tipo()))
                .count();
    }

    private static ResultadoExportacao novoResultado(String ficheiro, boolean gerado, int hinos,
                                                     int estrofes, int coros, List<String> erros,
                                                     List<String> avisos) {
        return new ResultadoExportacao(ficheiro, gerado, hinos, estrofes, coros,
                List.copyOf(erros), List.copyOf(avisos));
    }

    /** Hino em montagem (número, título e página do primeiro bloco). */
    private static final class HinoAtual {
        private final int numero;
        private final String titulo;
        private final int pagina;
        private final List<BlocoAnterior> blocos = new ArrayList<>();
        private boolean incompletaNoFim;

        private HinoAtual(int numero, String titulo, int pagina) {
            this.numero = numero;
            this.titulo = titulo;
            this.pagina = pagina;
        }

        int numero() { return numero; }
        String titulo() { return titulo; }
        int pagina() { return pagina; }
        List<BlocoAnterior> blocos() { return blocos; }
        boolean incompletaNoFim() { return incompletaNoFim; }
        void marcarIncompletaFim(boolean valor) { this.incompletaNoFim = valor; }
    }

    /** Bloco em montagem, antes de virar {@link BlocoNovo}. */
    private record BlocoAnterior(String tipo, Integer numero, List<LinhaNovo> linhas) {
    }
}
