package com.ravunana.hinario.leitura;

import com.ravunana.hinario.estrutura.EstruturaService;
import com.ravunana.hinario.pagina.Pagina;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints da leitura (PRD secção 7):
 * `POST /paginas/{n}/ler` põe a página na fila; `GET /fila` mostra o estado
 * da fila e as quotas por modelo; `POST /paginas/{n}/estruturar` (re)monta
 * a estrutura proposta de uma página já comparada.
 */
@RestController
public class LeituraController {

    private final FilaLeituraService fila;
    private final EstruturaService estruturas;

    public LeituraController(FilaLeituraService fila, EstruturaService estruturas) {
        this.fila = fila;
        this.estruturas = estruturas;
    }

    @PostMapping("/paginas/{numero}/ler")
    public ResponseEntity<Pagina> ler(@PathVariable int numero) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(fila.enfileirar(numero));
    }

    @PostMapping("/paginas/{numero}/estruturar")
    public ResponseEntity<Pagina> estruturar(@PathVariable int numero) {
        return ResponseEntity.ok(estruturas.estruturar(numero));
    }

    @GetMapping("/fila")
    public ResponseEntity<FilaEstado> fila() {
        return ResponseEntity.ok(fila.estado());
    }
}
