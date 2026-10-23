package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.PisoRequest;
import edu.eci.dosw.campusseguro.campus.dto.PisoResponse;
import edu.eci.dosw.campusseguro.campus.service.PisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// RF-02: los pisos se crean y listan desde su edificio; el resto de operaciones van por id del piso.
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PisoController {

    private final PisoService pisoService;

    @PostMapping("/buildings/{edificioId}/floors")
    public ResponseEntity<PisoResponse> crear(@PathVariable UUID edificioId,
                                              @Valid @RequestBody PisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pisoService.crear(edificioId, request));
    }

    @GetMapping("/buildings/{edificioId}/floors")
    public List<PisoResponse> listarPorEdificio(@PathVariable UUID edificioId) {
        return pisoService.listarPorEdificio(edificioId);
    }

    @GetMapping("/floors/{id}")
    public PisoResponse obtener(@PathVariable UUID id) {
        return pisoService.obtener(id);
    }

    @PutMapping("/floors/{id}")
    public PisoResponse modificar(@PathVariable UUID id,
                                  @Valid @RequestBody PisoRequest request) {
        return pisoService.modificar(id, request);
    }

    @PatchMapping("/floors/{id}/deactivate")
    public PisoResponse inactivar(@PathVariable UUID id) {
        return pisoService.inactivar(id);
    }
}
