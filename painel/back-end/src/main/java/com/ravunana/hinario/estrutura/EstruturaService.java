package com.ravunana.hinario.estrutura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ravunana.hinario.pagina.EstadoPagina;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.pagina.PaginaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

/**
 * Estruturação de páginas (PRD Fase 5): monta hinos, estrofes e coro
 * proposto a partir da leitura do Gemini, liga a página à anterior pela
 * regra 5.7.4 e marca estrofes incompletas. A ligação duvidosa fica
 * `ligacao.confirmar` — **nunca se junta por suposição**.
 */
@Service
public class EstruturaService {

    /** Só páginas já lidas/comparadas é que se estruturam (repetível). */
    private static final Set<EstadoPagina> ESTRUTURAVEIS = EnumSet.of(
            EstadoPagina.COMPARADA, EstadoPagina.ESTRUTURADA);

    private final Estruturador estruturador;
    private final PaginaService paginas;

    public EstruturaService(Estruturador estruturador, PaginaService paginas) {
        this.estruturador = estruturador;
        this.paginas = paginas;
    }

    /** Estrutura a página (estado → {@code estruturada}) e actualiza a anterior. */
    public Pagina estruturar(int numero) {
        Pagina pagina;
        try {
            pagina = paginas.obter(numero);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível ler a página " + numero + ": " + e.getMessage());
        }
        if (!ESTRUTURAVEIS.contains(pagina.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "estado " + pagina.estado().getValor() + " não permite estruturar");
        }
        JsonNode resposta = respostaGemini(pagina);
        if (resposta == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "página sem leitura do Gemini para estruturar");
        }

        ContextoHino contexto = contextoAnterior(numero);
        boolean proximoContinua = proximoContinua(numero);
        EstruturaPagina estrutura = estruturador.construir(resposta, contexto, proximoContinua);

        Pagina nova = pagina.comBlocos(estrutura.blocos()).comEstado(EstadoPagina.ESTRUTURADA);
        try {
            paginas.atualizar(nova);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível gravar a estrutura: " + e.getMessage());
        }

        if (numero > 1) {
            // a última estrofe da anterior fica incompleta se esta página continuar
            marcarIncompleta(numero - 1, estrutura.continuaDoAnterior());
        }
        return nova;
    }

    /** Resposta do Gemini da página, ou null se ainda não existe. */
    private static JsonNode respostaGemini(Pagina pagina) {
        if (pagina.leituras() == null || !pagina.leituras().isObject()) {
            return null;
        }
        JsonNode resposta = pagina.leituras().path("gemini").path("resposta");
        return resposta.isObject() ? resposta : null;
    }

    private ContextoHino contextoAnterior(int numero) {
        if (numero <= 1) {
            return null;
        }
        try {
            JsonNode resposta = respostaGemini(paginas.obter(numero - 1));
            return resposta == null ? null : estruturador.abrirContexto(resposta);
        } catch (Exception ignorada) {
            // sem página anterior legível: a ligação, se vier, pede confirmação
            return null;
        }
    }

    private boolean proximoContinua(int numero) {
        try {
            JsonNode blocos = paginas.obter(numero + 1).leituras()
                    .path("gemini").path("blocos");
            return blocos.isArray() && !blocos.isEmpty()
                    && blocos.get(0).path("continuacaoDeBlocoAnterior").asBoolean(false);
        } catch (Exception ignorada) {
            return false;
        }
    }

    /** Marca (ou limpa) a última estrofe da página como incompleta. */
    private void marcarIncompleta(int numero, boolean incompleta) {
        try {
            Pagina anterior = paginas.obter(numero);
            if (anterior.blocos() == null || !anterior.blocos().isArray() || anterior.blocos().isEmpty()) {
                return;
            }
            ArrayNode copia = anterior.blocos().deepCopy();
            for (int i = copia.size() - 1; i >= 0; i--) {
                JsonNode bloco = copia.get(i);
                if ("estrofe".equals(bloco.path("tipo").asText())) {
                    ((ObjectNode) bloco).put("incompleta", incompleta);
                    break;
                }
            }
            paginas.atualizar(anterior.comBlocos(copia));
        } catch (Exception ignorada) {
            // página anterior indisponível: a marca fica para a próxima passagem
        }
    }
}
