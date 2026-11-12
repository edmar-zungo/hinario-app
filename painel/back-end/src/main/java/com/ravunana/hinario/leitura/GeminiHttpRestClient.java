package com.ravunana.hinario.leitura;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Implementação real do {@link GeminiHttp} com o RestClient do Spring
 * (PRD 5.2: endpoint, cabeçalho {@code x-goog-api-key}).
 */
@Component
public class GeminiHttpRestClient implements GeminiHttp {

    private final GeminiConfig config;
    private final RestClient restClient;

    public GeminiHttpRestClient(GeminiConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(35));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Resposta gerarConteudo(String modelo, String corpoJson) throws GeminiApiExcecao {
        String uri = config.getUrlBase().replaceAll("/$", "")
                + "/v1beta/models/" + modelo + ":generateContent";
        try {
            byte[] corpo = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", config.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoJson)
                    .retrieve()
                    .toEntity(byte[].class)
                    .getBody();
            return new Resposta(200, corpo == null ? "" : new String(corpo, StandardCharsets.UTF_8));
        } catch (RestClientResponseException e) {
            return new Resposta(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.ERRO, 0,
                    "falha de rede: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }
}
