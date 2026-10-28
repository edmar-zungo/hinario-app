package com.ravunana.hinario.leitura;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
        this.restClient = RestClient.builder().build();
    }

    @Override
    public Resposta gerarConteudo(String modelo, String corpoJson) throws GeminiApiExcecao {
        String uri = config.getUrlBase().replaceAll("/$", "")
                + "/v1beta/models/" + modelo + ":generateContent";
        try {
            String corpo = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoJson)
                    .retrieve()
                    .toEntity(String.class)
                    .getBody();
            return new Resposta(200, corpo == null ? "" : corpo);
        } catch (RestClientResponseException e) {
            return new Resposta(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            throw new GeminiApiExcecao(GeminiApiExcecao.Tipo.ERRO, 0,
                    "falha de rede: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }
}
