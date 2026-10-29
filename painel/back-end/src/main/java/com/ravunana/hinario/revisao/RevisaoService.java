package com.ravunana.hinario.revisao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Set;

/**
 * Revisão humana (PRD Fase 6): gravar correções das linhas e confirmar.
 *
 * Regras de ouro: `leituras` (Gemini/Tesseract) **nunca** é tocado — o texto
 * original da leitura fica preservado mesmo depois de corrigir; as
 * correções vivem em `blocos`. O texto gravado é normalizado para NFC.
 */
@Service
public class RevisaoService {

    private static final Set<String> TIPOS = Set.of(
            "titulo_hino", "estrofe", "coro", "bloco_sem_numero");

    private final PaginaService paginas;
    private final DataFiles dataFiles;

    public RevisaoService(PaginaService paginas, DataFiles dataFiles) {
        this.paginas = paginas;
        this.dataFiles = dataFiles;
    }

    /**
     * {@code PUT /paginas/{n}/linhas}: grava os blocos corrigidos e passa a
     * página a `em_revisao`.
     */
    public Pagina guardarLinhas(int numero, JsonNode blocos) {
        Pagina pagina = carregar(numero);
        if (pagina.estado() != EstadoPagina.ESTRUTURADA
                && pagina.estado() != EstadoPagina.EM_REVISAO
                && pagina.estado() != EstadoPagina.CONFIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "a página está em '" + pagina.estado().getValor()
                            + "'; a revisão só começa em 'estruturada'");
        }
        ArrayNode validados = validar(blocos);
        try {
            Pagina nova = pagina.comBlocos(validados).comEstado(EstadoPagina.EM_REVISAO);
            paginas.atualizar(nova);
            return nova;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível gravar as correções", e);
        }
    }

    /**
     * {@code POST /paginas/{n}/confirmar}: marca todas as linhas como
     * `revisto` e a página como `confirmada`.
     */
    public Pagina confirmar(int numero) {
        Pagina pagina = carregar(numero);
        if (pagina.estado() != EstadoPagina.EM_REVISAO
                && pagina.estado() != EstadoPagina.CONFIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "só se confirma a partir de 'em_revisao'");
        }
        if (pagina.blocos() == null || !pagina.blocos().isArray() || pagina.blocos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "não há estrutura para confirmar; grave as linhas primeiro");
        }
        ArrayNode marcados = pagina.blocos().deepCopy();
        for (JsonNode bloco : marcados) {
            JsonNode linhas = bloco.get("linhas");
            if (linhas != null && linhas.isArray()) {
                for (JsonNode linha : linhas) {
                    ((ObjectNode) linha).put("revisto", true);
                }
            }
        }
        try {
            Pagina nova = pagina.comBlocos(marcados).comEstado(EstadoPagina.CONFIRMADA);
            paginas.atualizar(nova);
            return nova;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível confirmar a página", e);
        }
    }

    /** Estrutura enviada pelo painel: tipos válidos, recuo 0..2, texto NFC. */
    private ArrayNode validar(JsonNode blocos) {
        if (blocos == null || !blocos.isArray() || blocos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "envie a lista 'blocos' com a estrutura corrigida");
        }
        ArrayNode copia = blocos.deepCopy();
        for (JsonNode bloco : copia) {
            String tipo = bloco.path("tipo").asText("");
            if (!TIPOS.contains(tipo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "tipo de bloco inválido: '" + tipo + "'");
            }
            JsonNode linhas = bloco.get("linhas");
            if ("titulo_hino".equals(tipo)) {
                continue;
            }
            if (linhas == null || !linhas.isArray() || linhas.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "bloco '" + tipo + "' sem linhas");
            }
            for (JsonNode linha : linhas) {
                JsonNode texto = linha.get("texto");
                if (texto == null || !texto.isTextual() || texto.asText().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "linha sem texto");
                }
                ((ObjectNode) linha).put("texto",
                        Normalizer.normalize(texto.asText(), Normalizer.Form.NFC));
                int recuo = linha.path("recuo").asInt(0);
                if (recuo < 0 || recuo > 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "recuo fora de 0..2: " + recuo);
                }
            }
        }
        return copia;
    }

    private Pagina carregar(int numero) {
        try {
            return paginas.obter(numero);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "página " + numero + " não encontrada", e);
        }
    }
}
