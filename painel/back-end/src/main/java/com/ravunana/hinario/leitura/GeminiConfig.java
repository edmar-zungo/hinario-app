package com.ravunana.hinario.leitura;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuração da leitura com Gemini (PRD secção 2 e 5.8).
 *
 * A cadeia de modelos vem sempre daqui (`gemini.modelos`), nunca do código;
 * a chave vem da variável de ambiente `GEMINI_API_KEY` e nunca aparece no
 * painel nem nos registos.
 */
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiConfig {

    /** Interruptor geral (PRD 5.9): false = modo só-Tesseract. */
    private boolean enabled = true;

    /** Chave da API, só da variável de ambiente. Vazia = sem chave. */
    private String apiKey = "";

    /** Cadeia de modelos por ordem de preferência. */
    private List<String> modelos = new ArrayList<>(List.of(
            "gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite"));

    /** Repetições por modelo para 429 por minuto e erros temporários (5.8). */
    private int tentativasPorModelo = 2;

    /** Espera após 429 por minuto, em segundos (PRD: 65). */
    private int espera429MinutoSegundos = 65;

    /** Temperatura por omissão (PRD 5.2: 0 quando o modelo o permitir). */
    private double temperaturaPadrao = 0;

    /** Temperaturas por modelo, no formato `modelo:valor` separado por vírgulas. */
    private String temperaturas = "";

    /** Nível de raciocínio do modelo; vazio = omitir o campo. */
    private String thinkingLevel = "minimal";

    /** Fuso do reinício diário da quota (PRD 5.8; confirmar na documentação). */
    private String fusoQuota = "America/Los_Angeles";

    /** Base da API, alterável para testes. */
    private String urlBase = "https://generativelanguage.googleapis.com";

    /** Versão do prompt: chave do cache (PRD 5.8); subir ao mudar a instrução. */
    private int versaoPrompt = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getModelos() {
        return modelos;
    }

    public void setModelos(List<String> modelos) {
        this.modelos = new ArrayList<>(modelos);
    }

    public int getTentativasPorModelo() {
        return tentativasPorModelo;
    }

    public void setTentativasPorModelo(int tentativasPorModelo) {
        this.tentativasPorModelo = tentativasPorModelo;
    }

    public int getEspera429MinutoSegundos() {
        return espera429MinutoSegundos;
    }

    public void setEspera429MinutoSegundos(int espera429MinutoSegundos) {
        this.espera429MinutoSegundos = espera429MinutoSegundos;
    }

    public double getTemperaturaPadrao() {
        return temperaturaPadrao;
    }

    public void setTemperaturaPadrao(double temperaturaPadrao) {
        this.temperaturaPadrao = temperaturaPadrao;
    }

    public String getTemperaturas() {
        return temperaturas;
    }

    public void setTemperaturas(String temperaturas) {
        this.temperaturas = temperaturas;
    }

    public String getThinkingLevel() {
        return thinkingLevel;
    }

    public void setThinkingLevel(String thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }

    public String getFusoQuota() {
        return fusoQuota;
    }

    public void setFusoQuota(String fusoQuota) {
        this.fusoQuota = fusoQuota;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public int getVersaoPrompt() {
        return versaoPrompt;
    }

    public void setVersaoPrompt(int versaoPrompt) {
        this.versaoPrompt = versaoPrompt;
    }

    /** Sem chave, sem Gemini: o interruptor está ligado mas não há como pedir. */
    public boolean disponivel() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Temperatura do modelo, com omissão `gemini.temperatura-padrao`. */
    public double temperaturaDe(String modelo) {
        Map<String, Double> mapa = temperaturasModelo();
        return mapa.getOrDefault(modelo, temperaturaPadrao);
    }

    private Map<String, Double> temperaturasModelo() {
        Map<String, Double> mapa = new LinkedHashMap<>();
        if (temperaturas == null || temperaturas.isBlank()) {
            return mapa;
        }
        for (String parte : temperaturas.split(",")) {
            String[] campos = parte.trim().split(":", 2);
            if (campos.length == 2) {
                try {
                    mapa.put(campos[0].trim(), Double.parseDouble(campos[1].trim()));
                } catch (NumberFormatException ignorada) {
                    // entrada malformada: fica a temperatura por omissão
                }
            }
        }
        return mapa;
    }
}
