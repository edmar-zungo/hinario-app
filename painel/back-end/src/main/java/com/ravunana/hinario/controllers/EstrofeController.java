package com.ravunana.hinario.controllers;

import com.ravunana.hinario.model.Estrofe;
import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.services.EstrofeService;
import com.ravunana.hinario.services.HinoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/estrofe")
public class EstrofeController {

    private final EstrofeService estrofeService;


    public EstrofeController(EstrofeService estrofeService) {
        this.estrofeService = estrofeService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Estrofe>> getAllEstrofes() throws IOException {
        List<Estrofe> estrofes = estrofeService.getAllEstrofes();

        return ResponseEntity.status(HttpStatus.OK).body(estrofes);
    }

    @GetMapping("/{estrofeId}")
    public ResponseEntity<Estrofe> getEstrofeById(@PathVariable UUID estrofeId){
        Estrofe estrofeResult = estrofeService.getEstrofeById(estrofeId);

        return ResponseEntity.status(HttpStatus.OK).body(estrofeResult);
    }

    @PostMapping("/create")
    public ResponseEntity<Estrofe> createEstrofe(@RequestBody Estrofe estrofe){
        Estrofe estrofeResult = estrofeService.createEstrofe(estrofe);

        return ResponseEntity.status(HttpStatus.CREATED).body(estrofeResult);
    }

    @PutMapping("/update/{estrofeId}")
    public ResponseEntity<Estrofe> updateEstrofe(@PathVariable UUID estrofeId, @RequestBody Estrofe estrofe){
        Estrofe estrofeResult = estrofeService.updateEstrofe(estrofeId, estrofe);

        return ResponseEntity.status(HttpStatus.OK).body(estrofeResult);
    }

    @DeleteMapping("/delete/{estrofeId}")
    public ResponseEntity<String> deleteEstrofe(@PathVariable UUID estrofeId){
        estrofeService.deleteHino(estrofeId);

        return ResponseEntity.status(HttpStatus.OK).body("Estrofe eliminado!");
    }
}
