package com.ravunana.hinario.exportacao;

import com.ravunana.hinario.comparacao.ComparacaoHinoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints da Fase 7 (PRD secção 7):
 * {@code POST /exportar} valida e grava {@code hinario-<id>.json} só com
 * linhas revistas; {@code GET /comparar/{hinoId}} mostra a leitura nova
 * contra o JSON actual.
 */
@RestController
public class ExportacaoController {

    private final ExportacaoService exportacao;
    private final ComparacaoHinoService comparacao;

    public ExportacaoController(ExportacaoService exportacao, ComparacaoHinoService comparacao) {
        this.exportacao = exportacao;
        this.comparacao = comparacao;
    }

    @PostMapping("/exportar")
    public ResponseEntity<List<ExportacaoService.ResultadoExportacao>> exportar(
            @RequestParam(value = "hinarioId", required = false) String hinarioId) {
        return ResponseEntity.ok(exportacao.exportar(hinarioId));
    }

    @GetMapping("/comparar/{hinoId}")
    public ResponseEntity<ComparacaoHinoService.ComparacaoHino> comparar(
            @PathVariable UUID hinoId) {
        return ResponseEntity.ok(comparacao.comparar(hinoId));
    }
}
