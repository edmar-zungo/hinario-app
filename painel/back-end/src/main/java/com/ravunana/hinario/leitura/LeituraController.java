package com.ravunana.hinario.leitura;

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
 * da fila e as quotas por modelo.
 */
@RestController
public class LeituraController {

    private final FilaLeituraService fila;

    public LeituraController(FilaLeituraService fila) {
        this.fila = fila;
    }

    @PostMapping("/paginas/{numero}/ler")
    public ResponseEntity<Pagina> ler(@PathVariable int numero) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(fila.enfileirar(numero));
    }

    @GetMapping("/fila")
    public ResponseEntity<FilaEstado> fila() {
        return ResponseEntity.ok(fila.estado());
    }
}
