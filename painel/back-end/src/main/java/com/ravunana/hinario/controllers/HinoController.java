package com.ravunana.hinario.controllers;

import com.ravunana.hinario.model.Hino;
import com.ravunana.hinario.services.HinoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/hino")
public class HinoController {

    private final HinoService hinoService;


    public HinoController(HinoService hinoService) {
        this.hinoService = hinoService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Hino>> getAllHinos() throws IOException {
        List<Hino> hinos = hinoService.getAllHinos();

        return ResponseEntity.status(HttpStatus.OK).body(hinos);
    }

    @GetMapping("/{hinoId}")
    public ResponseEntity<Hino> getHinoById(@PathVariable UUID hinoId){
        Hino hinoResult = hinoService.getHinoById(hinoId);

        return ResponseEntity.status(HttpStatus.OK).body(hinoResult);
    }
    @GetMapping("/pagina/{pagina}")
    public ResponseEntity<Hino> getHinoByPagina(@PathVariable long pagina){
        Hino hinoResult = hinoService.getHinoByPagina(pagina);

        return ResponseEntity.status(HttpStatus.OK).body(hinoResult);
    }
    @GetMapping("/numero/{numero}")
    public ResponseEntity<Hino> getHinoByNumero(@PathVariable long numero){
        Hino hinoResult = hinoService.getHinoByNumero(numero);

        return ResponseEntity.status(HttpStatus.OK).body(hinoResult);
    }

    @PostMapping("/create")
    public ResponseEntity<Hino> createHino(@RequestBody Hino hino){
        Hino hinoReasult = hinoService.createHino(hino);

        return ResponseEntity.status(HttpStatus.CREATED).body(hinoReasult);
    }

    @PutMapping("/update/{hinoId}")
    public ResponseEntity<Hino> updateHino(@PathVariable UUID hinoId, @RequestBody Hino hino){
        Hino hinoResult = hinoService.updateHino(hinoId, hino);

        return ResponseEntity.status(HttpStatus.OK).body(hino);
    }

    @DeleteMapping("/delete/{hinoId}")
    public ResponseEntity<?> deleteHino(@PathVariable UUID hinoId){
        hinoService.deleteHino(hinoId);

        return ResponseEntity.status(HttpStatus.OK).body("Hino eliminado");
    }

    @PostMapping("favorito/add/{hinoId}")
    public ResponseEntity<Hino> addToFavorito(@PathVariable UUID hinoId){
        Hino hinoResult = hinoService.createHinoFavorito(hinoId);

        return ResponseEntity.status(HttpStatus.CREATED).body(hinoResult);
    }

    @GetMapping("favorito/all")
    public ResponseEntity<List<Hino>> getAllFavoritos(){
        List<Hino> hinosFavoritos = hinoService.getAllFavoritos();

        return ResponseEntity.status(HttpStatus.OK).body(hinosFavoritos);
    }
}
