package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEspacioRequest;
import edu.eci.dosw.campusseguro.campus.service.EspacioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EspacioController {

    private final EspacioService espacioService;

    @PostMapping("/floors/{pisoId}/spaces")
    public ResponseEntity<EspacioResponse> crear(@PathVariable UUID pisoId,
                                                 @Valid @RequestBody EspacioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crear(pisoId, request));
    }

    @GetMapping("/floors/{pisoId}/spaces")
    public List<EspacioResponse> listarPorPiso(@PathVariable UUID pisoId) {
        return espacioService.listarPorPiso(pisoId);
    }

    @GetMapping("/buildings/{edificioId}/spaces")
    public List<EspacioResponse> listarPorEdificio(@PathVariable UUID edificioId) {
        return espacioService.listarPorEdificio(edificioId);
    }

    @GetMapping("/spaces/{id}")
    public EspacioResponse obtener(@PathVariable UUID id) {
        return espacioService.obtener(id);
    }

    @PutMapping("/spaces/{id}")
    public EspacioResponse modificar(@PathVariable UUID id,
                                     @Valid @RequestBody ModificarEspacioRequest request) {
        return espacioService.modificar(id, request);
    }
}
