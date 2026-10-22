package edu.eci.dosw.campusseguro.campus.ruta.controller;

import edu.eci.dosw.campusseguro.campus.ruta.dto.CambiarEstadoRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.CrearRutaRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evacuation-routes")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @PostMapping
    public ResponseEntity<RutaResponse> crear(@Valid @RequestBody CrearRutaRequest request) {
        RutaResponse creada = rutaService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/evacuation-routes/" + creada.id())).body(creada);
    }

    @PatchMapping("/{id}/status")
    public RutaResponse cambiarEstado(@PathVariable Long id,
                                      @Valid @RequestBody CambiarEstadoRequest request) {
        return rutaService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}")
    public RutaResponse obtener(@PathVariable Long id) {
        return rutaService.obtener(id);
    }

    @GetMapping
    public List<RutaResponse> listar() {
        return rutaService.listar();
    }
}
