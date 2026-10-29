package com.ravunana.hinario.comparacao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração do segundo leitor (PRD secção 3): o motor Tesseract corre em
 * local, a partir do binário {@code tesseract} no PATH.
 */
@Component
@ConfigurationProperties(prefix = "tesseract")
public class TesseractConfig {

    /** Interruptor geral: false = leitura só com o Gemini. */
    private boolean enabled = true;

    /** Código do idioma dos {@code .traineddata} (por omissão: português). */
    private String idioma = "por";

    /** Binário do Tesseract (caminho ou nome no PATH). */
    private String binario = "tesseract";

    /** Pasta das tabelas {@code .traineddata} (vazia = variável TESSDATA_PREFIX do sistema). */
    private String datapath = "";

    /** Page segmentation mode (6 = bloco uniforme de texto). */
    private int psm = 6;

    /** Timeout por página, em segundos. */
    private int timeoutSegundos = 120;

    /** Se o segundo leitor está utilizável. */
    public boolean disponivel() {
        return enabled && idioma != null && !idioma.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getBinario() {
        return binario;
    }

    public void setBinario(String binario) {
        this.binario = binario;
    }

    public String getDatapath() {
        return datapath;
    }

    public void setDatapath(String datapath) {
        this.datapath = datapath;
    }

    public int getPsm() {
        return psm;
    }

    public void setPsm(int psm) {
        this.psm = psm;
    }

    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public void setTimeoutSegundos(int timeoutSegundos) {
        this.timeoutSegundos = timeoutSegundos;
    }
}
