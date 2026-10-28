package com.ravunana.hinario.leitura;

/**
 * Chamada HTTP única à API do Gemini. Separada do {@link GeminiProvider}
 * para que os testes possam simular 429, bloqueios e respostas inválidas
 * sem rede (aceitação da Fase 3: 429 forçado).
 */
public interface GeminiHttp {

    /** Resposta da API: código HTTP e corpo. */
    record Resposta(int status, String corpo) {
    }

    /**
     * {@code POST <urlBase>/v1beta/models/{modelo}:generateContent}.
     *
     * @throws GeminiApiExcecao se falhar antes de chegar uma resposta HTTP
     */
    Resposta gerarConteudo(String modelo, String corpoJson) throws GeminiApiExcecao;
}
