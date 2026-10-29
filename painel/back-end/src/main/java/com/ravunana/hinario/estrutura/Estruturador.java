package com.ravunana.hinario.estrutura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Montagem determinística dos blocos do Gemini (5.4) na estrutura da página
 * (PRD Fase 5):
 *
 * - `titulo_hino` inicia um hino; blocos sem título no início continuam o hino
 *   aberto da página anterior;
 * - `bloco_sem_numero` entre estrofes numeradas, com itálico ou recuo, vira
 *   **coro proposto** (`possivelCoro`, regra 5.7.6) — o humano confirma;
 * - ligação entre páginas validada por 5.7.4: sequência 1, 2, 3… sem buracos;
 *   fora disso, `ligacao.confirmar` em vez de juntar por suposição;
 * - estrofe que continua na página seguinte fica `incompleta`.
 *
 * Nunca altera texto: copia `linhas` e `duvidas` verbatim do Gemini.
 */
@Component
public class Estruturador {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Estrutura a página com o contexto da anterior e o aviso da seguinte. */
    public EstruturaPagina construir(JsonNode resposta, ContextoHino ctx, boolean proximoContinua) {
        ArrayNode blocos = MAPPER.createArrayNode();

        Integer numeroHino = ctx == null ? null : ctx.numeroHino();
        String titulo = ctx == null ? null : ctx.titulo();
        Integer ultimaEstrofe = ctx == null ? null : ctx.ultimaEstrofe();
        Integer proximaEsperada = null;
        boolean continuaDoAnterior = false;

        JsonNode geminiBlocos = resposta == null ? null : resposta.get("blocos");
        int total = geminiBlocos == null || !geminiBlocos.isArray() ? 0 : geminiBlocos.size();
        for (int i = 0; i < total; i++) {
            JsonNode bloco = geminiBlocos.get(i);
            String tipo = bloco.path("tipo").asText("");
            boolean primeiro = i == 0;

            switch (tipo) {
                case "titulo_hino" -> {
                    numeroHino = bloco.path("numeroHino").isInt() ? bloco.path("numeroHino").asInt() : null;
                    titulo = bloco.path("titulo").isTextual() ? bloco.path("titulo").asText() : null;
                    ultimaEstrofe = null;
                    proximaEsperada = null;
                    ObjectNode registo = base("titulo_hino", bloco);
                    numeroOuNull(registo, "numeroHino", numeroHino);
                    if (titulo == null) {
                        registo.putNull("titulo");
                    } else {
                        registo.put("titulo", titulo);
                    }
                    registo.putNull("numeroEstrofe");
                    registo.putNull("ligacao");
                    registo.putArray("linhas");
                    registo.set("duvidas", copiar(bloco, "duvidas"));
                    blocos.add(registo);
                }
                case "estrofe" -> {
                    Integer n = bloco.path("numeroEstrofe").isInt()
                            ? bloco.path("numeroEstrofe").asInt() : null;
                    boolean emContinuacao = primeiro && bloco.path("continuacaoDeBlocoAnterior").asBoolean(false);
                    ObjectNode registo = estrofe(bloco, n, numeroHino, emContinuacao);

                    if (emContinuacao) {
                        continuaDoAnterior = true;
                        if (numeroHino == null) {
                            numeroHino = ctx == null ? null : ctx.numeroHino();
                        }
                        if (titulo == null) {
                            titulo = ctx == null ? null : ctx.titulo();
                        }
                        numeroOuNull(registo, "numeroHino", numeroHino);
                        boolean ligacaoOk = ctx != null && ctx.ultimaEstrofe() != null
                                && n != null && n == ctx.ultimaEstrofe() + 1;
                        ObjectNode ligacao = registo.putObject("ligacao");
                        numeroOuNull(ligacao, "hinoAnterior", ctx == null ? null : ctx.numeroHino());
                        numeroOuNull(ligacao, "estrofeAnterior", ctx == null ? null : ctx.ultimaEstrofe());
                        numeroOuNull(ligacao, "esperado",
                                ctx == null || ctx.ultimaEstrofe() == null ? null : ctx.ultimaEstrofe() + 1);
                        numeroOuNull(ligacao, "encontrado", n);
                        ligacao.put("confirmar", !ligacaoOk);
                        proximaEsperada = n == null ? null : n + 1;
                        ultimaEstrofe = n == null ? ultimaEstrofe : n;
                    } else {
                        if (n == null) {
                            registo.put("sequenciaSuspeita", true);
                        } else {
                            if (proximaEsperada != null && n != proximaEsperada) {
                                registo.put("sequenciaSuspeita", true);
                            }
                            proximaEsperada = n + 1;
                            ultimaEstrofe = n;
                        }
                    }
                    blocos.add(registo);
                }
                case "bloco_sem_numero" -> {
                    boolean italico = bloco.path("italico").asBoolean(false);
                    boolean recuo = temRecuo(bloco);
                    boolean entreEstrofes = ultimaEstrofe != null
                            && eEstrofeNumerada(bemNoProximo(geminiBlocos, i));
                    boolean possivelCoro = entreEstrofes && (italico || recuo);
                    ObjectNode registo = base(possivelCoro ? "coro" : "bloco_sem_numero", bloco);
                    registo.put("possivelCoro", possivelCoro);
                    numeroOuNull(registo, "numeroHino", numeroHino);
                    registo.putNull("titulo");
                    registo.putNull("numeroEstrofe");
                    registo.putNull("ligacao");
                    registo.set("linhas", copiar(bloco, "linhas"));
                    registo.set("duvidas", copiar(bloco, "duvidas"));
                    blocos.add(registo);
                }
                default -> {
                    // tipo desconhecido: nunca juntar por suposição — copia como bloco_sem_numero
                    ObjectNode registo = base("bloco_sem_numero", bloco);
                    registo.putNull("numeroHino");
                    registo.putNull("titulo");
                    registo.putNull("numeroEstrofe");
                    registo.putNull("ligacao");
                    registo.set("linhas", copiar(bloco, "linhas"));
                    registo.set("duvidas", copiar(bloco, "duvidas"));
                    blocos.add(registo);
                }
            }
        }

        if (proximoContinua) {
            marcarUltimaEstrofeIncompleta(blocos);
        }
        return new EstruturaPagina(blocos, new ContextoHino(numeroHino, titulo, ultimaEstrofe),
                continuaDoAnterior);
    }

    /** Hino aberto no fim da página, a partir da resposta do Gemini (5.4). */
    public ContextoHino abrirContexto(JsonNode resposta) {
        Integer numeroHino = null;
        String titulo = null;
        Integer ultimaEstrofe = null;
        JsonNode geminiBlocos = resposta == null ? null : resposta.get("blocos");
        if (geminiBlocos == null || !geminiBlocos.isArray()) {
            return null;
        }
        for (JsonNode bloco : geminiBlocos) {
            String tipo = bloco.path("tipo").asText("");
            if ("titulo_hino".equals(tipo)) {
                numeroHino = bloco.path("numeroHino").isInt() ? bloco.path("numeroHino").asInt() : null;
                titulo = bloco.path("titulo").isTextual() ? bloco.path("titulo").asText() : null;
                ultimaEstrofe = null;
            } else if ("estrofe".equals(tipo) && bloco.path("numeroEstrofe").isInt()) {
                ultimaEstrofe = bloco.path("numeroEstrofe").asInt();
            }
        }
        if (numeroHino == null && ultimaEstrofe == null) {
            return null;
        }
        return new ContextoHino(numeroHino, titulo, ultimaEstrofe);
    }

    private static ObjectNode estrofe(JsonNode bloco, Integer n, Integer numeroHino, boolean continuacao) {
        ObjectNode registo = base("estrofe", bloco);
        numeroOuNull(registo, "numeroHino", numeroHino);
        registo.putNull("titulo");
        numeroOuNull(registo, "numeroEstrofe", n);
        registo.put("continuacao", continuacao);
        registo.put("incompleta", false);
        registo.put("sequenciaSuspeita", false);
        registo.putNull("ligacao");
        registo.set("linhas", copiar(bloco, "linhas"));
        registo.set("duvidas", copiar(bloco, "duvidas"));
        return registo;
    }

    private static ObjectNode base(String tipo, JsonNode origem) {
        ObjectNode registo = MAPPER.createObjectNode();
        registo.put("tipo", tipo);
        registo.put("italico", origem.path("italico").asBoolean(false));
        registo.put("continuacao", false);
        registo.put("incompleta", false);
        registo.put("sequenciaSuspeita", false);
        registo.put("possivelCoro", false);
        return registo;
    }

    private static void numeroOuNull(ObjectNode no, String campo, Integer valor) {
        if (valor == null) {
            no.putNull(campo);
        } else {
            no.put(campo, valor);
        }
    }

    private static JsonNode copiar(JsonNode bloco, String campo) {
        JsonNode valor = bloco.get(campo);
        return valor == null ? MAPPER.createArrayNode() : valor.deepCopy();
    }

    private static boolean temRecuo(JsonNode bloco) {
        JsonNode linhas = bloco.get("linhas");
        if (linhas == null || !linhas.isArray()) {
            return false;
        }
        for (JsonNode linha : linhas) {
            if (linha.path("recuo").asInt(0) > 0) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode bemNoProximo(JsonNode blocos, int i) {
        return i + 1 < blocos.size() ? blocos.get(i + 1) : null;
    }

    private static boolean eEstrofeNumerada(JsonNode bloco) {
        return bloco != null && "estrofe".equals(bloco.path("tipo").asText(""))
                && bloco.path("numeroEstrofe").isInt();
    }

    private static void marcarUltimaEstrofeIncompleta(ArrayNode blocos) {
        for (int i = blocos.size() - 1; i >= 0; i--) {
            JsonNode bloco = blocos.get(i);
            if ("estrofe".equals(bloco.path("tipo").asText())) {
                ((ObjectNode) bloco).put("incompleta", true);
                return;
            }
        }
    }
}
