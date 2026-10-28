package com.ravunana.hinario.migracao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * {@code POST /migracao} — corre a migração e gera o relatório (PRD secção 7).
 */
@RestController
@RequestMapping("/migracao")
public class MigracaoController {

    private final MigracaoService migracao;

    public MigracaoController(MigracaoService migracao) {
        this.migracao = migracao;
    }

    @PostMapping
    public ResponseEntity<?> migrar() {
        try {
            ResultadoMigracao resultado = migracao.migrar();
            return ResponseEntity.ok(resultado);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", e.getMessage() == null ? "falha na migração" : e.getMessage()));
        }
    }
}
