package com.ravunana.hinario.leitura;

import com.ravunana.hinario.estrutura.EstruturaService;
import com.ravunana.hinario.pagina.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints da leitura:
 * {@code POST /paginas/{n}/ler} executa a leitura síncrona com Gemini;
 * {@code POST /paginas/{n}/estruturar} (re)monta a estrutura proposta.
 */
@RestController
public class LeituraController {

    private final LeituraService leitura;
    private final EstruturaService estruturas;

    public LeituraController(LeituraService leitura, EstruturaService estruturas) {
        this.leitura = leitura;
        this.estruturas = estruturas;
    }

    @PostMapping("/paginas/{numero}/ler")
    public ResponseEntity<Pagina> ler(@PathVariable int numero) {
        return ResponseEntity.ok(leitura.ler(numero));
    }

    @PostMapping("/paginas/{numero}/estruturar")
    public ResponseEntity<Pagina> estruturar(@PathVariable int numero) {
        return ResponseEntity.ok(estruturas.estruturar(numero));
    }
}
