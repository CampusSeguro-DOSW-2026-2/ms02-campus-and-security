package co.edu.escuelaing.campusseguro.campus.controller;

import co.edu.escuelaing.campusseguro.campus.dto.ZonaSeguraRequest;
import co.edu.escuelaing.campusseguro.campus.dto.ZonaSeguraResponse;
import co.edu.escuelaing.campusseguro.campus.service.ZonaSeguraService;
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
    public ZonaSeguraResponse modificar(@PathVariable Long id,
                                        @Valid @RequestBody ZonaSeguraRequest request) {
        return zonaSeguraService.modificar(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ZonaSeguraResponse inactivar(@PathVariable Long id) {
        return zonaSeguraService.inactivar(id);
    }

    @GetMapping
    public List<ZonaSeguraResponse> listar() {
        return zonaSeguraService.listar();
    }

    @GetMapping("/{id}")
    public ZonaSeguraResponse obtener(@PathVariable Long id) {
        return zonaSeguraService.obtener(id);
    }
}