package com.ravunana.hinario.leitura;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.pagina.Pagina;
import com.ravunana.hinario.services.DataFiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Rotação de modelos do Gemini (PRD 5.8 e Anexo A):
 *
 * 1. primeiro modelo da cadeia que não está pausado;
 * 2. 429 por minuto: espera e repete no mesmo modelo até
 *    {@code gemini.tentativas-por-modelo}, depois pausa-o brevemente e passa
 *    ao seguinte;
 * 3. 429 por dia: pausa o modelo até ao reinício diário e passa ao seguinte;
 * 4. 503/erro temporário: espera crescente e repete, depois passa;
 * 5. todos pausados: {@link QuotaEsgotadaExcecao} — a fila retoma sozinha;
 * 6. nunca misturar modelos na mesma página (cada tentativa relê a página de
 *    início);
 * 7. {@code modeloUsado} e tentativas ficam registados na página.
 *
 * O cache (hash da imagem + versão do prompt + modelo) é consultado antes de
 * cada chamada: acerto não gasta quota.
 */
@Component
public class ModelPool {

    private final GeminiConfig config;
    private final GeminiPedido pedido;
    private final EstadoQuota quota;
    private final CacheLeitura cache;
    private final ContextoPagina contexto;
    private final DataFiles dataFiles;

    public ModelPool(GeminiConfig config, GeminiPedido pedido, EstadoQuota quota,
                     CacheLeitura cache, ContextoPagina contexto, DataFiles dataFiles) {
        this.config = config;
        this.pedido = pedido;
        this.quota = quota;
        this.cache = cache;
        this.contexto = contexto;
        this.dataFiles = dataFiles;
    }

    private enum Fim {
        OK, BLOQUEADO, PAUSADO_DIA, FALHA
    }

    private record ResultadoModelo(Fim fim, JsonNode resposta) {
    }

    public LeituraResultado ler(Pagina pagina) throws LeituraExcecao {
        if (!config.disponivel()) {
            throw new LeituraExcecao(config.isEnabled()
                    ? "GEMINI_API_KEY não definida"
                    : "Gemini desactivado (gemini.enabled=false)", List.of());
        }
        byte[] jpeg = imagem(pagina);
        String contextoTexto = contexto.construir(pagina);
        List<LeituraResultado.Tentativa> tentativas = new ArrayList<>();

        int tentados = 0;
        int bloqueados = 0;
        int falhas = 0;
        for (String modelo : config.getModelos()) {
            if (quota.pausado(modelo)) {
                continue;
            }
            tentados++;
            ResultadoModelo resultado = tentarModelo(modelo, pagina.hash(), jpeg, contextoTexto, tentativas);
            if (resultado.fim() == Fim.OK) {
                return new LeituraResultado("gemini", modelo, tentativas, resultado.resposta());
            }
            if (resultado.fim() == Fim.BLOQUEADO) {
                bloqueados++;
            } else if (resultado.fim() == Fim.FALHA) {
                falhas++;
            }
        }

        int pausados = config.getModelos().size() - tentados;
        if (pausados == 0 && falhas == 0 && bloqueados == config.getModelos().size()) {
            throw new BloqueadaExcecao(tentativas);
        }
        if (quota.primeiroDisponivel() == null) {
            throw new QuotaEsgotadaExcecao(tentativas);
        }
        if (falhas > 0) {
            throw new LeituraExcecao("falha de leitura em todos os modelos da cadeia", tentativas);
        }
        if (bloqueados > 0) {
            throw new BloqueadaExcecao(tentativas);
        }
        throw new LeituraExcecao("nenhum modelo devolveu leitura", tentativas);
    }

    private ResultadoModelo tentarModelo(String modelo, String hashImagem, byte[] jpeg,
                                         String contextoTexto, List<LeituraResultado.Tentativa> tentativas) {
        int tentativas429 = 0;
        int temporarios = 0;
        int invalidas = 0;
        String chave = cache.chave(hashImagem, modelo);
        int limite = Math.max(1, config.getTentativasPorModelo());

        while (true) {
            if (cache.contem(chave)) {
                JsonNode guardada = cache.resposta(chave);
                if (guardada != null) {
                    tentativas.add(LeituraResultado.Tentativa.cache(modelo, "ok"));
                    return new ResultadoModelo(Fim.OK, guardada);
                }
            }
            try {
                quota.registarPedido(modelo);
                JsonNode resposta = pedido.pedir(modelo, jpeg, contextoTexto);
                cache.guardar(chave, modelo, resposta);
                tentativas.add(LeituraResultado.Tentativa.api(modelo, "ok"));
                return new ResultadoModelo(Fim.OK, resposta);
            } catch (GeminiApiExcecao e) {
                switch (e.getTipo()) {
                    case QUOTA_DIARIA:
                        tentativas.add(LeituraResultado.Tentativa.api(modelo, "429"));
                        quota.pausarAteReinicioDiario(modelo);
                        return new ResultadoModelo(Fim.PAUSADO_DIA, null);
                    case QUOTA_MINUTO:
                        tentativas.add(LeituraResultado.Tentativa.api(modelo, "429"));
                        if (++tentativas429 >= limite) {
                            // pausa curta: evita martelar o mesmo modelo a cada página
                            quota.pausarMinuto(modelo, config.getEspera429MinutoSegundos());
                            return new ResultadoModelo(Fim.FALHA, null);
                        }
                        if (!dormir(config.getEspera429MinutoSegundos())) {
                            return new ResultadoModelo(Fim.FALHA, null);
                        }
                        break;
                    case TEMPORARIO:
                        tentativas.add(LeituraResultado.Tentativa.api(modelo, "erro"));
                        if (++temporarios >= limite) {
                            return new ResultadoModelo(Fim.FALHA, null);
                        }
                        if (!dormir(config.getEspera429MinutoSegundos() * temporarios)) {
                            return new ResultadoModelo(Fim.FALHA, null);
                        }
                        break;
                    case RESPOSTA_INVALIDA:
                        tentativas.add(LeituraResultado.Tentativa.api(modelo, "erro"));
                        if (++invalidas > 1) {
                            // 5.6: repetir uma vez no mesmo modelo, depois passar
                            return new ResultadoModelo(Fim.FALHA, null);
                        }
                        break;
                    case BLOQUEADO:
                        tentativas.add(LeituraResultado.Tentativa.api(modelo, "bloqueado"));
                        return new ResultadoModelo(Fim.BLOQUEADO, null);
                    case ERRO:
                    default:
                        tentativas.add(LeituraResultado.Tentativa.api(modelo, "erro"));
                        return new ResultadoModelo(Fim.FALHA, null);
                }
            }
        }
    }

    private byte[] imagem(Pagina pagina) throws LeituraExcecao {
        try {
            return Files.readAllBytes(dataFiles.resolve(pagina.imagemProcessada()));
        } catch (IOException e) {
            throw new LeituraExcecao("imagem processada em falta para a página "
                    + pagina.pagina() + ": " + e.getMessage(), List.of());
        }
    }

    /** Espera em segundos; devolve false se for interrompida (desligar). */
    private static boolean dormir(int segundos) {
        if (segundos <= 0) {
            return true;
        }
        try {
            Thread.sleep(segundos * 1000L);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
