package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.service.ZonaSeguraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/safe-zones")
@RequiredArgsConstructor
public class ZonaSeguraController {

    private final ZonaSeguraService zonaSeguraService;

    @PostMapping
    public ResponseEntity<ZonaSeguraResponse> crear(@Valid @RequestBody ZonaSeguraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaSeguraService.crear(request));
    }

    @PutMapping("/{id}")
    public ZonaSeguraResponse modificar(@PathVariable UUID id,
                                        @Valid @RequestBody ZonaSeguraRequest request) {
        return zonaSeguraService.modificar(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ZonaSeguraResponse inactivar(@PathVariable UUID id) {
        return zonaSeguraService.inactivar(id);
    }

    @GetMapping
    public List<ZonaSeguraResponse> listar() {
        return zonaSeguraService.listar();
    }

    @GetMapping("/{id}")
    public ZonaSeguraResponse obtener(@PathVariable UUID id) {
        return zonaSeguraService.obtener(id);
    }
}