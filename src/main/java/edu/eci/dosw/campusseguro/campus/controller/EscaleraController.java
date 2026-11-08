package edu.eci.dosw.campusseguro.campus.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.CambiarDisponibilidadEscaleraRequest;
import edu.eci.dosw.campusseguro.campus.dto.EscaleraRequest;
import edu.eci.dosw.campusseguro.campus.dto.EscaleraResponse;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import edu.eci.dosw.campusseguro.campus.service.EscaleraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Escaleras", description = "RF-04. Escaleras y pisos que conectan dentro de un edificio.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EscaleraController {

    private final EscaleraService escaleraService;

    @Operation(summary = "Registrar escalera",
            description = "Registra una escalera conectando al menos dos pisos activos del edificio. El código se genera automáticamente.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PostMapping("/buildings/{edificioId}/stairs")
    public ResponseEntity<EscaleraResponse> crear(
            @PathVariable UUID edificioId,
            @Valid @RequestBody EscaleraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(escaleraService.crear(edificioId, request));
    }

    @Operation(summary = "Listar escaleras de un edificio")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/buildings/{edificioId}/stairs")
    public List<EscaleraResponse> listarPorEdificio(
            @PathVariable UUID edificioId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return escaleraService.listarPorEdificio(edificioId, incluirInactivas);
    }

    @Operation(summary = "Consultar escalera")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/stairs/{id}")
    public EscaleraResponse obtener(@PathVariable UUID id) {
        return escaleraService.obtener(id);
    }

    @Operation(summary = "Cambiar disponibilidad de una escalera",
            description = "Coordinación puede bloquear o habilitar una escalera. Para estados no disponibles, el motivo es obligatorio.")
    @Permiso(Permisos.STAIR_STATUS_WRITE)
    @PatchMapping("/stairs/{id}/status")
    public EscaleraResponse cambiarDisponibilidad(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarDisponibilidadEscaleraRequest request) {
        return escaleraService.cambiarDisponibilidad(id, request);
    }

    @Operation(summary = "Inactivar escalera")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/stairs/{id}/deactivate")
    public EscaleraResponse inactivar(@PathVariable UUID id) {
        return escaleraService.inactivar(id);
    }

    @Operation(summary = "Reactivar escalera")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/stairs/{id}/reactivate")
    public EscaleraResponse reactivar(@PathVariable UUID id) {
        return escaleraService.reactivar(id);
    }
}
