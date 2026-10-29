package com.ravunana.hinario.revisao;

import com.fasterxml.jackson.databind.JsonNode;
import com.ravunana.hinario.pagina.Pagina;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints da revisão (PRD secção 7 — Fase 6):
 * `PUT /paginas/{n}/linhas` grava as correções; `POST /paginas/{n}/confirmar`
 * marca as linhas como `revisto`.
 */
@RestController
public class RevisaoController {

    private final RevisaoService revisao;

    public RevisaoController(RevisaoService revisao) {
        this.revisao = revisao;
    }

    @PutMapping("/paginas/{numero}/linhas")
    public ResponseEntity<Pagina> guardarLinhas(
            @PathVariable int numero, @RequestBody JsonNode blocos) {
        return ResponseEntity.ok(revisao.guardarLinhas(numero, blocos));
    }

    @PostMapping("/paginas/{numero}/confirmar")
    public ResponseEntity<Pagina> confirmar(@PathVariable int numero) {
        return ResponseEntity.ok(revisao.confirmar(numero));
    }
}
