package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado da quota por modelo, em {@code quota/estado.json} (PRD 3.1 e 5.8):
 * contadores de pedidos por minuto e por dia, e {@code pausadoAte}.
 * Guardado em disco para sobreviver a reinícios.
 */
@Component
public class EstadoQuota {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DataFiles dataFiles;
    private final GeminiConfig config;
    private final Clock clock;
    private Map<String, ModeloQuota> modelos = new LinkedHashMap<>();

    @Autowired
    public EstadoQuota(DataFiles dataFiles, GeminiConfig config) {
        this(dataFiles, config, Clock.systemUTC());
    }

    /** Relógio controlável, apenas para os testes. */
    public EstadoQuota(DataFiles dataFiles, GeminiConfig config, Clock clock) {
        this.dataFiles = dataFiles;
        this.config = config;
        this.clock = clock;
        carregar();
    }

    /** Contadores e pausa de um modelo (formato de `quota/estado.json`). */
    public static class ModeloQuota {
        private int pedidosMinuto;
        private int pedidosDia;
        private String janelaMinuto = "";
        private String janelaDia = "";
        private String pausadoAte;

        public int getPedidosMinuto() {
            return pedidosMinuto;
        }

        public void setPedidosMinuto(int pedidosMinuto) {
            this.pedidosMinuto = pedidosMinuto;
        }

        public int getPedidosDia() {
            return pedidosDia;
        }

        public void setPedidosDia(int pedidosDia) {
            this.pedidosDia = pedidosDia;
        }

        public String getJanelaMinuto() {
            return janelaMinuto;
        }

        public void setJanelaMinuto(String janelaMinuto) {
            this.janelaMinuto = janelaMinuto;
        }

        public String getJanelaDia() {
            return janelaDia;
        }

        public void setJanelaDia(String janelaDia) {
            this.janelaDia = janelaDia;
        }

        public String getPausadoAte() {
            return pausadoAte;
        }

        public void setPausadoAte(String pausadoAte) {
            this.pausadoAte = pausadoAte;
        }
    }

    /** Instante até ao qual o modelo está pausado, ou null. */
    public synchronized Instant pausadoAte(String modelo) {
        String valor = modelo(modelo).getPausadoAte();
        return valor == null ? null : Instant.parse(valor);
    }

    public synchronized boolean pausado(String modelo) {
        Instant ate = pausadoAte(modelo);
        return ate != null && ate.isAfter(clock.instant());
    }

    /** Pausa até ao próximo reinício diário no fuso da quota (PRD 5.8.3). */
    public synchronized void pausarAteReinicioDiario(String modelo) {
        ZoneId fuso = ZoneId.of(config.getFusoQuota());
        String dia = clock.instant().atZone(fuso).toLocalDate().format(DIA);
        modelo(modelo).setJanelaDia(dia);
        modelo(modelo).setPausadoAte(
                clock.instant().atZone(fuso).toLocalDate().plusDays(1)
                        .atStartOfDay(fuso).toInstant().toString());
        guardar();
    }

    /** Pausa curta após esgotar as tentativas de 429 por minuto (PRD 5.8.2). */
    public synchronized void pausarMinuto(String modelo, int segundos) {
        modelo(modelo).setPausadoAte(clock.instant().plusSeconds(Math.max(1, segundos)).toString());
        guardar();
    }

    /** Conta um pedido feito à API (o cache não conta: não gasta quota). */
    public synchronized void registarPedido(String modelo) {
        Instant agora = clock.instant();
        String minutoAtual = String.valueOf(agora.getEpochSecond() / 60);
        ModeloQuota quota = modelo(modelo);
        if (!minutoAtual.equals(quota.getJanelaMinuto())) {
            quota.setJanelaMinuto(minutoAtual);
            quota.setPedidosMinuto(0);
        }
        String diaAtual = agora.atZone(ZoneId.of(config.getFusoQuota())).toLocalDate().format(DIA);
        if (!diaAtual.equals(quota.getJanelaDia())) {
            quota.setJanelaDia(diaAtual);
            quota.setPedidosDia(0);
        }
        quota.setPedidosMinuto(quota.getPedidosMinuto() + 1);
        quota.setPedidosDia(quota.getPedidosDia() + 1);
        guardar();
    }

    /** Primeiro modelo da cadeia que não está pausado (PRD 5.8.1). */
    public synchronized String primeiroDisponivel() {
        for (String modelo : config.getModelos()) {
            if (!pausado(modelo)) {
                return modelo;
            }
        }
        return null;
    }

    /** Instante mais cedo de retoma entre os modelos pausados, ou null. */
    public synchronized Instant proximaRetoma() {
        Instant maisCedo = null;
        for (String modelo : config.getModelos()) {
            Instant ate = pausadoAte(modelo);
            if (ate != null && ate.isAfter(clock.instant())
                    && (maisCedo == null || ate.isBefore(maisCedo))) {
                maisCedo = ate;
            }
        }
        return maisCedo;
    }

    /** Instantâneo para {@code GET /fila}. */
    public synchronized Map<String, ModeloQuota> snapshot() {
        Map<String, ModeloQuota> copia = new LinkedHashMap<>();
        for (String modelo : config.getModelos()) {
            ModeloQuota quota = modelo(modelo);
            ModeloQuota outra = new ModeloQuota();
            outra.setPedidosMinuto(quota.getPedidosMinuto());
            outra.setPedidosDia(quota.getPedidosDia());
            outra.setJanelaMinuto(quota.getJanelaMinuto());
            outra.setJanelaDia(quota.getJanelaDia());
            outra.setPausadoAte(quota.getPausadoAte());
            copia.put(modelo, outra);
        }
        return copia;
    }

    private ModeloQuota modelo(String nome) {
        return modelos.computeIfAbsent(nome, n -> new ModeloQuota());
    }

    private void carregar() {
        try {
            java.nio.file.Path caminho = caminho();
            if (!Files.exists(caminho)) {
                return;
            }
            JsonNode raiz = dataFiles.mapper().readTree(caminho.toFile());
            JsonNode modelosJson = raiz.path("modelos");
            if (modelosJson.isObject()) {
                Map<String, ModeloQuota> carregados = new LinkedHashMap<>();
                modelosJson.fields().forEachRemaining(entrada -> {
                    ModeloQuota quota = dataFiles.mapper().convertValue(entrada.getValue(), ModeloQuota.class);
                    carregados.put(entrada.getKey(), quota == null ? new ModeloQuota() : quota);
                });
                modelos = carregados;
            }
        } catch (IOException e) {
            // estado de quota corrompido: começa em zero (não bloqueia a leitura)
            modelos = new LinkedHashMap<>();
        }
    }

    private void guardar() {
        try {
            Map<String, Object> raiz = Map.of("modelos", modelos);
            dataFiles.escreverJson(caminho(), raiz);
        } catch (IOException e) {
            // falha ao persistir não deve parar a fila; tenta na próxima mutação
        }
    }

    private java.nio.file.Path caminho() {
        return dataFiles.subpasta("quota").resolve("estado.json");
    }
}
