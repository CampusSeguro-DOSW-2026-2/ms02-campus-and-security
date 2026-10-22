package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EdificioResponse;
import edu.eci.dosw.campusseguro.campus.service.EdificioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class EdificioController {

    private final EdificioService edificioService;

    @PostMapping
    public ResponseEntity<EdificioResponse> crear(@Valid @RequestBody EdificioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(edificioService.crear(request));
    }

    @PutMapping("/{id}")
    public EdificioResponse modificar(@PathVariable UUID id,
                                      @Valid @RequestBody EdificioRequest request) {
        return edificioService.modificar(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public EdificioResponse inactivar(@PathVariable UUID id) {
        return edificioService.inactivar(id);
    }

    @GetMapping
    public List<EdificioResponse> listar() {
        return edificioService.listar();
    }

    @GetMapping("/{id}")
    public EdificioResponse obtener(@PathVariable UUID id) {
        return edificioService.obtener(id);
    }
}