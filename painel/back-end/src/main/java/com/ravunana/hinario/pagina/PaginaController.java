package com.ravunana.hinario.pagina;

import com.ravunana.hinario.services.DataFiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code POST /paginas}, {@code GET /paginas} e {@code GET /paginas/{n}}
 * (PRD secção 7 — Fase 2).
 *
 * No POST, os números de página vêm em {@code paginas} (lista separada por
 * vírgulas, pela ordem dos ficheiros) ou, com um só ficheiro, em
 * {@code pagina}. Sem nenhum dos dois, lê o número do nome do ficheiro
 * (ex.: {@code 0395.jpg}).
 */
@RestController
@RequestMapping("/paginas")
public class PaginaController {

    private static final Pattern DIGITOS = Pattern.compile("(\\d+)");

    private final PaginaService paginas;
    private final DataFiles dataFiles;

    public PaginaController(PaginaService paginas, DataFiles dataFiles) {
        this.paginas = paginas;
        this.dataFiles = dataFiles;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Pagina>> importar(
            @RequestParam("ficheiros") List<MultipartFile> ficheiros,
            @RequestParam(value = "pagina", required = false) Integer pagina,
            @RequestParam(value = "paginas", required = false) String numeros,
            @RequestParam("hinarioId") String hinarioId,
            @RequestParam(value = "rotacao", defaultValue = "0") double rotacao) {

        List<Pagina> importadas = paginas.importar(emparelhar(ficheiros, pagina, numeros), hinarioId, rotacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(importadas);
    }

    @GetMapping
    public ResponseEntity<List<Pagina>> listar() throws IOException {
        return ResponseEntity.ok(paginas.listar());
    }

    @GetMapping("/{numero}")
    public ResponseEntity<Pagina> obter(@PathVariable int numero) throws IOException {
        return ResponseEntity.ok(paginas.obter(numero));
    }

    /**
     * A foto para o ecrã de revisão (Fase 6). Por omissão a versão processada
     * (a que os leitores viram); com {@code original=true}, o ficheiro intacto.
     */
    @GetMapping("/{numero}/imagem")
    public ResponseEntity<byte[]> imagem(
            @PathVariable int numero,
            @RequestParam(value = "original", defaultValue = "false") boolean original) throws IOException {

        Pagina pagina = paginas.obter(numero);
        String relativo = original ? pagina.imagem() : pagina.imagemProcessada();
        Path ficheiro = dataFiles.resolve(relativo).normalize();
        if (!ficheiro.startsWith(dataFiles.raiz()) || !Files.isRegularFile(ficheiro)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "imagem em falta");
        }
        MediaType tipo = relativo.toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(tipo).body(Files.readAllBytes(ficheiro));
    }

    private static List<PaginaService.PedidoImportacao> emparelhar(
            List<MultipartFile> ficheiros, Integer pagina, String numeros) {

        if (ficheiros == null || ficheiros.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nenhum ficheiro submetido");
        }
        List<Integer> numerosResolvidos;
        if (numeros != null && !numeros.isBlank()) {
            numerosResolvidos = interpretarLista(numeros);
        } else if (ficheiros.size() == 1 && pagina != null) {
            numerosResolvidos = List.of(pagina);
        } else {
            numerosResolvidos = new ArrayList<>();
            for (MultipartFile ficheiro : ficheiros) {
                numerosResolvidos.add(numeroDoNome(ficheiro.getOriginalFilename()));
            }
        }
        if (numerosResolvidos.size() != ficheiros.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "a lista 'paginas' tem " + numerosResolvidos.size() + " números para "
                            + ficheiros.size() + " ficheiros");
        }

        List<PaginaService.PedidoImportacao> pedidos = new ArrayList<>();
        for (int i = 0; i < ficheiros.size(); i++) {
            pedidos.add(new PaginaService.PedidoImportacao(ficheiros.get(i), numerosResolvidos.get(i)));
        }
        return pedidos;
    }

    private static List<Integer> interpretarLista(String numeros) {
        List<Integer> valores = new ArrayList<>();
        for (String parte : numeros.split(",")) {
            try {
                valores.add(Integer.parseInt(parte.trim()));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "número de página inválido: " + parte.trim());
            }
        }
        return valores;
    }

    private static int numeroDoNome(String nomeOriginal) {
        if (nomeOriginal == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "indique o número de página (parâmetro 'pagina' ou 'paginas')");
        }
        Matcher digitos = DIGITOS.matcher(nomeOriginal);
        if (!digitos.find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "não foi possível ler o número de página do ficheiro '" + nomeOriginal + "'");
        }
        return Integer.parseInt(digitos.group(1));
    }
}
