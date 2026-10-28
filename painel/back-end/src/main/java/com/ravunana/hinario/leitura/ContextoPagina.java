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
 * Contexto enviado junto da imagem (PRD 5.5).
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
        contexto.append("Contexto (NÃO transcrever; serve só para o campo \"continuacaoDeBlocoAnterior\"):\n");
        contexto.append("- Esta é a página ").append(pagina.pagina())
                .append(" do hinário \"").append(lingua.getNome()).append("\".\n");
        HinoAberto aberto = hinoAbertoNaPaginaAnterior(pagina.pagina());
        if (aberto == null) {
            contexto.append("- Não há hino aberto da página anterior.\n");
        } else {
            contexto.append("- Hino aberto no fim da página anterior: nº ").append(aberto.numeroHino())
                    .append(", \"").append(aberto.titulo()).append("\", última estrofe: ")
                    .append(aberto.ultimaEstrofe()).append(".\n");
        }
        contexto.append("Transcreve apenas o que vês na imagem, seguindo as regras.");
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
            JsonNode resposta = anterior.path("leituras").path("gemini").path("resposta");
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
