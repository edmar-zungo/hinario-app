package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.comparacao.AlfabetoVerificador;
import com.ravunana.hinario.comparacao.ComparacaoService;
import com.ravunana.hinario.comparacao.TesseractProvider;
import com.ravunana.hinario.estrutura.EstruturaService;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Fila sequencial de leitura (PRD 5.8 "Fila" e secção 7): um pedido de cada
 * vez, prioridade por número de página.
 *
 * Estados: `em_fila` → `lida` → `comparada` (com as duas leituras);
 * com quota esgotada as páginas ficam `pausada_quota` e **retomam sozinhas**;
 * falhas definitivas vão para `erro_leitura` ou `bloqueada_pela_ia` (que
 * "segue só com Tesseract", PRD 5.6).
 */
@Service
public class FilaLeituraService {

    /** Estados a partir dos quais se pode (re)ler uma página. */
    private static final Set<EstadoPagina> LECIVEIS = EnumSet.of(
            EstadoPagina.IMPORTADA, EstadoPagina.EM_FILA, EstadoPagina.PAUSADA_QUOTA,
            EstadoPagina.LIDA, EstadoPagina.ERRO_LEITURA, EstadoPagina.BLOQUEADA_PELA_IA);

    private final NavigableSet<Integer> fila = new TreeSet<>();
    private final Object trava = new Object();

    private final GeminiProvider gemini;
    private final GeminiConfig configGemini;
    private final EstadoQuota quota;
    private final TesseractProvider tesseract;
    private final ComparacaoService comparacao;
    private final AlfabetoVerificador alfabeto;
    private final EstruturaService estruturas;
    private final PaginaService paginas;
    private final DataFiles dataFiles;
    private final long retomaMs;

    private Thread trabalhador;
    private volatile boolean parado;
    private volatile boolean quotaEsgotada;

    public FilaLeituraService(GeminiProvider gemini, GeminiConfig configGemini, EstadoQuota quota,
                              TesseractProvider tesseract, ComparacaoService comparacao,
                              AlfabetoVerificador alfabeto, EstruturaService estruturas,
                              PaginaService paginas, DataFiles dataFiles,
                              @Value("${hinario.fila.retoma-ms:15000}") long retomaMs) {
        this.gemini = gemini;
        this.configGemini = configGemini;
        this.quota = quota;
        this.tesseract = tesseract;
        this.comparacao = comparacao;
        this.alfabeto = alfabeto;
        this.estruturas = estruturas;
        this.paginas = paginas;
        this.dataFiles = dataFiles;
        this.retomaMs = Math.max(50, retomaMs);
    }

    /** Põe uma página na fila (PRD `POST /paginas/{n}/ler`). */
    public Pagina enfileirar(int numero) {
        Pagina pagina;
        try {
            pagina = paginas.obter(numero);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível ler a página " + numero + ": " + e.getMessage());
        }
        if (!gemini.disponivel() && !tesseract.disponivel()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, configGemini.isEnabled()
                    ? "GEMINI_API_KEY não definida e Tesseract indisponível — leitura indisponível"
                    : "Gemini desactivado e Tesseract indisponível");
        }
        if (!LECIVEIS.contains(pagina.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "estado " + pagina.estado().getValor() + " não permite nova leitura");
        }

        Pagina emFila = pagina.comEstado(EstadoPagina.EM_FILA);
        try {
            paginas.atualizar(emFila);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível gravar a página: " + e.getMessage());
        }
        synchronized (trava) {
            fila.add(numero);
            trava.notifyAll();
        }
        return emFila;
    }

    /** Instantâneo para {@code GET /fila}. */
    public FilaEstado estado() {
        List<FilaEstado.PaginaFila> pendentes = new ArrayList<>();
        try {
            for (Pagina pagina : paginas.listar()) {
                if (pagina.estado() == EstadoPagina.EM_FILA || pagina.estado() == EstadoPagina.PAUSADA_QUOTA) {
                    pendentes.add(new FilaEstado.PaginaFila(
                            pagina.pagina(), pagina.hinarioId(), pagina.estado().getValor()));
                }
            }
        } catch (IOException ignorada) {
            // lista indisponível: mostrar só a fila em memória
        }
        pendentes.sort(java.util.Comparator.comparingInt(FilaEstado.PaginaFila::pagina));

        List<FilaEstado.ModeloFila> modelos = new ArrayList<>();
        Map<String, EstadoQuota.ModeloQuota> snapshot = quota.snapshot();
        snapshot.forEach((modelo, q) -> {
            Instant ate = q.getPausadoAte() == null ? null : Instant.parse(q.getPausadoAte());
            boolean pausado = ate != null && ate.isAfter(Instant.now());
            modelos.add(new FilaEstado.ModeloFila(modelo, pausado,
                    pausado ? q.getPausadoAte() : null, q.getPedidosMinuto(), q.getPedidosDia()));
        });

        String estado = !gemini.disponivel() && !tesseract.disponivel() ? "indisponivel"
                : quotaEsgotada || pendentes.stream().anyMatch(p -> "pausada_quota".equals(p.estado()))
                        ? "pausada_quota"
                        : pendentes.isEmpty() ? "ociosa" : "a_processar";
        Instant retoma = quota.proximaRetoma();
        return new FilaEstado(estado, pendentes.size(), gemini.disponivel(), tesseract.disponivel(),
                quotaEsgotada, retoma == null ? null : retoma.toString(), pendentes, modelos);
    }

    /** Arranca a fila no arranque da aplicação (retoma o que ficou `em_fila`). */
    @EventListener(ApplicationReadyEvent.class)
    public void arrancar() {
        synchronized (trava) {
            if (trabalhador != null) {
                return;
            }
            parado = false;
            try {
                for (Pagina pagina : paginas.listar()) {
                    if (pagina.estado() == EstadoPagina.EM_FILA) {
                        fila.add(pagina.pagina());
                    }
                }
            } catch (IOException ignorada) {
                // sem páginas para retomar
            }
            reporPausadas();
            trabalhador = new Thread(this::trabalhar, "fila-leitura");
            trabalhador.setDaemon(true);
            trabalhador.start();
        }
    }

    @PreDestroy
    public void parar() {
        parado = true;
        synchronized (trava) {
            trava.notifyAll();
        }
        if (trabalhador != null) {
            trabalhador.interrupt();
        }
    }

    private void trabalhar() {
        while (!parado) {
            synchronized (trava) {
                while (!parado && fila.isEmpty()) {
                    try {
                        trava.wait(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (parado) {
                    return;
                }
            }

            if (gemini.disponivel() && quota.primeiroDisponivel() == null) {
                // PRD 5.8.5: a fila pára, as páginas ficam pausada_quota,
                // e retoma sozinha quando a quota voltar
                quotaEsgotada = true;
                marcarPendentes(EstadoPagina.PAUSADA_QUOTA);
                dormir(retomaMs);
                reporPausadas();
                continue;
            }

            Integer numero;
            synchronized (trava) {
                numero = fila.pollFirst();
            }
            if (numero != null) {
                processar(numero);
            }
        }
    }

    private void processar(int numero) {
        Pagina pagina;
        try {
            pagina = paginas.obter(numero);
        } catch (Exception e) {
            return;
        }
        if (!LECIVEIS.contains(pagina.estado())) {
            return;
        }

        if (!gemini.disponivel()) {
            // modo só-Tesseract (PRD 5.9)
            gravar(pagina, null, null);
            return;
        }

        try {
            LeituraResultado resultado = gemini.ler(pagina);
            quotaEsgotada = false;
            gravar(pagina, resultado, null);
        } catch (QuotaEsgotadaExcecao e) {
            quotaEsgotada = true;
            try {
                paginas.atualizar(pagina
                        .comLeituras(comLeituras(pagina.leituras(), geminiJson(null, e)))
                        .comEstado(EstadoPagina.PAUSADA_QUOTA));
            } catch (IOException ignorada) {
                // segue para a fila marcar os pendentes
            }
            synchronized (trava) {
                fila.add(numero);
                trava.notifyAll();
            }
        } catch (BloqueadaExcecao e) {
            // PRD 5.6: bloqueada_pela_ia "segue só com Tesseract"
            gravar(pagina, null, e);
        } catch (LeituraExcecao e) {
            try {
                paginas.atualizar(pagina
                        .comLeituras(comLeituras(pagina.leituras(), geminiJson(null, e)))
                        .comEstado(EstadoPagina.ERRO_LEITURA));
            } catch (IOException ignorada) {
                // idem
            }
        }
    }

    /**
     * Grava a página com as leituras disponíveis: Tesseract em paralelo quando
     * existir (PRD fluxo 2); com as duas leituras, compara e passa a
     * `comparada`. O Gemini nunca é bloqueio único do Tesseract.
     */
    private void gravar(Pagina pagina, LeituraResultado leituraGemini, LeituraExcecao falhaGemini) {
        boolean soTesseract = leituraGemini == null && falhaGemini == null;
        ObjectNode leituras = dataFiles.mapper().createObjectNode();
        if (pagina.leituras() != null && pagina.leituras().isObject()) {
            pagina.leituras().fields().forEachRemaining(c -> leituras.set(c.getKey(), c.getValue()));
        }
        if (leituraGemini != null || falhaGemini != null) {
            leituras.set("gemini", geminiJson(leituraGemini, falhaGemini));
        }

        EstadoPagina estado = leituraGemini != null ? EstadoPagina.LIDA
                : falhaGemini instanceof BloqueadaExcecao ? EstadoPagina.BLOQUEADA_PELA_IA
                : soTesseract ? EstadoPagina.LIDA
                : EstadoPagina.ERRO_LEITURA;
        ArrayNode divergencias = null;

        if (tesseract.disponivel()) {
            try {
                LeituraResultado leituraTesseract = tesseract.ler(pagina);
                leituras.set("tesseract", tesseractJson(leituraTesseract));
                if (leituraGemini != null) {
                    divergencias = comparacao.comparar(leituraGemini.resposta(), leituraTesseract.resposta());
                    estado = EstadoPagina.COMPARADA;
                }
            } catch (LeituraExcecao e) {
                // o segundo leitor falhou: a página fica com a leitura principal
                ObjectNode erro = leituras.putObject("tesseract");
                erro.putArray("linhas");
                erro.put("erro", e.getMessage() == null ? "falha do Tesseract" : e.getMessage());
            }
        }

        if (leituraGemini != null) {
            ObjectNode geminiNo = (ObjectNode) leituras.get("gemini");
            ObjectNode validacao = geminiNo.putObject("validacao");
            validacao.set("alfabeto", alfabeto.verificarResposta(leituraGemini.resposta()));
        }

        try {
            Pagina nova = pagina.comLeituras(leituras);
            if (leituraGemini != null) {
                nova = nova.comDivergencias(divergencias == null
                        ? dataFiles.mapper().createArrayNode() : divergencias);
            }
            paginas.atualizar(nova.comEstado(estado));
            if (estado == EstadoPagina.COMPARADA) {
                // fluxo 4.3: o sistema compara e propõe a estrutura
                try {
                    estruturas.estruturar(pagina.pagina());
                } catch (Exception ignorada) {
                    // estruturação falhou: a página fica comparada
                }
            }
        } catch (IOException e) {
            // disco falhou depois da leitura: a página fica como estava
        }
    }

    /** Todas as páginas na fila passam a {@code estado} (em disco). */
    private void marcarPendentes(EstadoPagina estado) {
        List<Integer> alvos;
        synchronized (trava) {
            alvos = new ArrayList<>(fila);
        }
        for (int numero : alvos) {
            try {
                Pagina pagina = paginas.obter(numero);
                if (pagina.estado() != estado && pagina.estado() == EstadoPagina.EM_FILA) {
                    paginas.atualizar(pagina.comEstado(estado));
                }
            } catch (Exception ignorada) {
                // página removida entretanto
            }
        }
    }

    /** Quando a quota volta: `pausada_quota` → `em_fila` e reentram na fila. */
    private void reporPausadas() {
        if (quota.primeiroDisponivel() == null) {
            return;
        }
        quotaEsgotada = false;
        try {
            for (Pagina pagina : paginas.listar()) {
                if (pagina.estado() == EstadoPagina.PAUSADA_QUOTA) {
                    paginas.atualizar(pagina.comEstado(EstadoPagina.EM_FILA));
                    synchronized (trava) {
                        fila.add(pagina.pagina());
                    }
                }
            }
        } catch (IOException ignorada) {
            // sem páginas para repor
        }
    }

    private JsonNode comLeituras(JsonNode atuais, ObjectNode gemini) {
        ObjectNode raiz = dataFiles.mapper().createObjectNode();
        if (atuais != null && atuais.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> campos = atuais.fields();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                raiz.set(campo.getKey(), campo.getValue());
            }
        }
        raiz.set("gemini", gemini);
        return raiz;
    }

    private ObjectNode geminiJson(LeituraResultado resultado, LeituraExcecao falha) {
        ObjectNode gemini = dataFiles.mapper().createObjectNode();
        List<LeituraResultado.Tentativa> tentativas;
        if (resultado != null) {
            gemini.put("modelo", resultado.modelo());
            tentativas = resultado.tentativas();
            gemini.set("resposta", resultado.resposta());
        } else {
            tentativas = falha == null ? List.of() : falha.getTentativas();
            String ultimoModelo = tentativas.isEmpty() ? null : tentativas.get(tentativas.size() - 1).modelo();
            if (ultimoModelo != null) {
                gemini.put("modelo", ultimoModelo);
            }
            if (falha != null) {
                gemini.put("erro", falha.getMessage() == null ? "falha de leitura" : falha.getMessage());
            }
        }
        ArrayNode array = gemini.putArray("tentativas");
        for (LeituraResultado.Tentativa t : tentativas) {
            ObjectNode registo = array.addObject();
            registo.put("modelo", t.modelo());
            registo.put("resultado", t.resultado());
            registo.put("quando", t.quando());
            registo.put("origem", t.origem());
        }
        return gemini;
    }

    /** {@code leituras.tesseract} no formato do PRD 3.2: {linhas, quando}. */
    private ObjectNode tesseractJson(LeituraResultado resultado) {
        ObjectNode no = dataFiles.mapper().createObjectNode();
        no.set("linhas", resultado.resposta().get("linhas"));
        if (!resultado.tentativas().isEmpty()) {
            no.put("quando", resultado.tentativas().get(0).quando());
        }
        return no;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
