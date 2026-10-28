package com.ravunana.hinario.leitura;

import com.ravunana.hinario.pagina.Pagina;
import org.springframework.stereotype.Component;

/**
 * Fornecedor de leitura principal (PRD secção 0 e 2): lê com a cadeia de
 * modelos do {@link ModelPool}, com o pedido 5.2, a instrução 5.3, o esquema
 * 5.4 e o contexto 5.5.
 */
@Component
public class GeminiProvider implements OcrProvider {

    private final GeminiConfig config;
    private final ModelPool pool;

    public GeminiProvider(GeminiConfig config, ModelPool pool) {
        this.config = config;
        this.pool = pool;
    }

    @Override
    public String nome() {
        return "gemini";
    }

    @Override
    public boolean disponivel() {
        return config.disponivel();
    }

    @Override
    public LeituraResultado ler(Pagina pagina) throws LeituraExcecao {
        return pool.ler(pagina);
    }
}
