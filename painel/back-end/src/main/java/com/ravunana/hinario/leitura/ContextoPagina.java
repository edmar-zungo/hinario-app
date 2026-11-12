package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.enums.Linguas;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contexto compacto enviado junto da imagem.
 *
 * Serve só para o modelo marcar {@code continuacaoDeBlocoAnterior}: **nunca**
 * é usado para preencher texto (a validação da ligação é na Fase 5, PRD 5.7.4).
 */
@Component
public class ContextoPagina {

    private final DataFiles dataFiles;

    public ContextoPagina(DataFiles dataFiles) {
        this.dataFiles = dataFiles;
    }

    public String construir(Pagina pagina) {
        Linguas lingua = Linguas.porCodigo(pagina.hinarioId());
        StringBuilder contexto = new StringBuilder();
        contexto.append("Página ").append(pagina.pagina()).append(", ")
                .append(lingua.getCodigo()).append(". ");
        HinoAberto aberto = hinoAbertoNaPaginaAnterior(pagina.pagina());
        if (aberto == null) {
            contexto.append("Sem continuação anterior.");
        } else {
            contexto.append("Continuação: hino ").append(aberto.numeroHino())
                    .append(", estrofe ").append(aberto.ultimaEstrofe()).append(". ");
        }
        return contexto.toString();
    }

    /** Hino ainda aberto ao fim da página anterior, se o modelo já o leu. */
    private HinoAberto hinoAbertoNaPaginaAnterior(int numero) {
        if (numero <= 1) {
            return null;
        }
        Path ficheiro = dataFiles.subpasta("paginas").resolve(String.format("%04d.json", numero - 1));
        if (!Files.exists(ficheiro)) {
            return null;
        }
        try {
            JsonNode anterior = dataFiles.mapper().readTree(ficheiro.toFile());
            JsonNode leituras = anterior.path("leituras");
            JsonNode resposta = leituras.path("gemini").path("resposta");
            if (resposta.isMissingNode()) {
                resposta = leituras.path("groq").path("resposta");
            }
            JsonNode blocos = resposta.path("blocos");
            if (!blocos.isArray() || blocos.isEmpty()) {
                return null;
            }
            Integer numeroHino = null;
            String titulo = null;
            Integer ultimaEstrofe = null;
            for (JsonNode bloco : blocos) {
                if (bloco.path("numeroHino").canConvertToInt()) {
                    numeroHino = bloco.path("numeroHino").asInt();
                }
                if (bloco.path("titulo").isTextual()) {
                    titulo = bloco.path("titulo").asText();
                }
                if (bloco.path("numeroEstrofe").canConvertToInt()) {
                    ultimaEstrofe = bloco.path("numeroEstrofe").asInt();
                }
            }
            if (numeroHino == null || ultimaEstrofe == null) {
                return null;
            }
            return new HinoAberto(numeroHino, titulo == null ? "?" : titulo, ultimaEstrofe);
        } catch (IOException e) {
            return null;
        }
    }

    private record HinoAberto(int numeroHino, String titulo, int ultimaEstrofe) {
    }
}
