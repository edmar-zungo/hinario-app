package com.ravunana.hinario.leitura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** HTTP guionizado para os testes: uma resposta por chamada, contando chamadas. */
final class HttpFalso implements GeminiHttp {

    private final List<Resposta> guion = Collections.synchronizedList(new ArrayList<>());
    final List<String> modelos = Collections.synchronizedList(new ArrayList<>());

    synchronized void responder(Resposta... respostas) {
        guion.clear();
        guion.addAll(List.of(respostas));
    }

    @Override
    public Resposta gerarConteudo(String modelo, String corpoJson) {
        modelos.add(modelo);
        synchronized (this) {
            if (guion.isEmpty()) {
                return RespostasGemini.status(500, "sem guiao para o teste");
            }
            return guion.remove(0);
        }
    }

    int chamadas() {
        return modelos.size();
    }
}
