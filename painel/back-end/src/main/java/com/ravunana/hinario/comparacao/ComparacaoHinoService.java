package com.ravunana.hinario.comparacao;

import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.exportacao.BlocoNovo;
import com.ravunana.hinario.exportacao.ExportacaoService;
import com.ravunana.hinario.exportacao.HinoNovo;
import com.ravunana.hinario.exportacao.LinhaNovo;
import com.ravunana.hinario.model.Estrofe;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.services.EstrofeService;
import com.ravunana.hinario.services.HinoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * {@code GET /comparar/{hinoId}} (PRD Fase 7): a leitura nova (páginas
 * confirmadas do painel) contra o JSON actual ({@code hinario.json} +
 * {@code estrofes.json}), com as diferenças classificadas — igual,
 * so_acento, so_pontuacao, divergente, so_na_leitura, so_no_actual.
 *
 * Serve para corrigir erros da migração e preencher hinos que o JSON actual
 * tem incompletos. Só compara — nunca altera texto.
 */
@Service
public class ComparacaoHinoService {

    private final HinoService hinos;
    private final EstrofeService estrofes;
    private final ExportacaoService exportacao;
    private final AlinhamentoLinhas alinhamento;
    private final ClassificadorLinha classificador;

    @Autowired
    public ComparacaoHinoService(HinoService hinos, EstrofeService estrofes,
                                 ExportacaoService exportacao) {
        this(hinos, estrofes, exportacao, new AlinhamentoLinhas(), new ClassificadorLinha());
    }

    ComparacaoHinoService(HinoService hinos, EstrofeService estrofes,
                          ExportacaoService exportacao, AlinhamentoLinhas alinhamento,
                          ClassificadorLinha classificador) {
        this.hinos = hinos;
        this.estrofes = estrofes;
        this.exportacao = exportacao;
        this.alinhamento = alinhamento;
        this.classificador = classificador;
    }

    /** Uma linha de cada lado, alinhada, com a classificação da diferença. */
    public record LinhaComparada(
            String actual, String nova, String tipo,
            Integer blocoActual, Integer linhaActual, Integer blocoNova, Integer linhaNova) {
    }

    /** Resposta de {@code GET /comparar/{hinoId}}. */
    public record ComparacaoHino(
            UUID hinoId, long numero, String lingua, String titulo,
            boolean temLeituraNova,
            List<BlocoNovo> actual, List<BlocoNovo> nova,
            List<LinhaComparada> diferencas, int linhasIguais) {
    }

    public ComparacaoHino comparar(UUID hinoId) {
        Hino hino;
        try {
            hino = hinos.getHinoById(hinoId);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "hino não existe: " + hinoId, e);
        }
        Linguas lingua = hino.getLingua();
        if (lingua == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "hino sem língua associada");
        }

        List<BlocoNovo> actual = blocosDoActual(hino);
        HinoNovo montado = exportacao.hinoMontado(lingua.getCodigo(), hino.getNumero()).orElse(null);
        List<BlocoNovo> nova = montado == null ? List.of() : montado.blocos();

        List<LinhaComparada> diferencas = new ArrayList<>();
        int iguais = 0;

        List<Lado> ladosActual = achatar(actual, true);
        List<Lado> ladosNova = achatar(nova, false);
        List<String> textosActual = ladosActual.stream().map(Lado::texto).toList();
        List<String> textosNova = ladosNova.stream().map(Lado::texto).toList();

        for (AlinhamentoLinhas.Par par : alinhamento.alinhar(textosActual, textosNova)) {
            Lado a = par.gemini() >= 0 ? ladosActual.get(par.gemini()) : null;
            Lado n = par.tesseract() >= 0 ? ladosNova.get(par.tesseract()) : null;
            String tipo;
            if (a == null) {
                tipo = "so_no_novo";
            } else if (n == null) {
                tipo = "so_na_leitura";
            } else {
                tipo = classificador.classificar(a.texto(), n.texto());
            }
            if ("igual".equals(tipo)) {
                iguais++;
                continue;
            }
            diferencas.add(new LinhaComparada(
                    a == null ? null : a.texto(), n == null ? null : n.texto(), tipo,
                    a == null ? null : a.bloco(), a == null ? null : a.linha(),
                    n == null ? null : n.bloco(), n == null ? null : n.linha()));
        }

        return new ComparacaoHino(hinoId, hino.getNumero(), lingua.getCodigo(),
                hino.getTitulo(), montado != null, actual, nova, diferencas, iguais);
    }

    /** Blocos do JSON actual: estrofes na ordem do ficheiro, coro sem número. */
    private List<BlocoNovo> blocosDoActual(Hino hino) {
        List<BlocoNovo> blocos = new ArrayList<>();
        try {
            for (Estrofe estrofe : estrofes.getAllEstrofes()) {
                if (estrofe.getHino() == null || !hino.getId().equals(estrofe.getHino().getId())) {
                    continue;
                }
                String texto = estrofe.getDescricao() == null ? "" : estrofe.getDescricao();
                List<LinhaNovo> linhas = Arrays.stream(texto.split("\n", -1))
                        .map(LinhaNovo::de).toList();
                blocos.add(estrofe.isRefrao()
                        ? BlocoNovo.coro(linhas)
                        : BlocoNovo.estrofe((int) estrofe.getNumero(), linhas));
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "estrofes.json indisponível: " + e.getMessage(), e);
        }
        return blocos;
    }

    /** Uma linha com a sua posição (bloco, linha) de cada lado. */
    private record Lado(int bloco, int linha, String texto) {
    }

    private static List<Lado> achatar(List<BlocoNovo> blocos, boolean actual) {
        List<Lado> lados = new ArrayList<>();
        for (int b = 0; b < blocos.size(); b++) {
            List<LinhaNovo> linhas = blocos.get(b).linhas();
            for (int l = 0; l < linhas.size(); l++) {
                lados.add(new Lado(b, l, linhas.get(l).texto()));
            }
        }
        return lados;
    }
}
