package edu.eci.dosw.campusseguro.campus.controller;

import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.service.ZonaSeguraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Zonas seguras",
    description = "RF-04. Zonas seguras o puntos de encuentro a los que se evacúa."
)
@RestController
@RequestMapping("/api/v1/safe-zones")
@RequiredArgsConstructor
public class ZonaSeguraController {

    private final ZonaSeguraService zonaSeguraService;

    @Operation(
        summary = "Registrar zona segura",
        description = "Solo ADMIN. La capacidad debe ser mayor que cero. La zona queda activa."
    )
    @Permiso(Permisos.SAFE_ZONE_WRITE)
    @PostMapping
    public ResponseEntity<ZonaSeguraResponse> crear(
            @Valid @RequestBody ZonaSeguraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(zonaSeguraService.crear(request));
    }

    @Operation(
        summary = "Modificar zona segura",
        description = "Solo ADMIN."
    )
    @Permiso(Permisos.SAFE_ZONE_WRITE)
    @PutMapping("/{id}")
    public ZonaSeguraResponse modificar(
            @PathVariable UUID id,
            @Valid @RequestBody ZonaSeguraRequest request) {
        return zonaSeguraService.modificar(id, request);
    }

    @Operation(
        summary = "Inactivar zona segura",
        description = "Solo ADMIN. Se rechaza con 422 si la zona es destino de rutas activas; deben bloquearse antes."
    )
    @Permiso(Permisos.SAFE_ZONE_WRITE)
    @PatchMapping("/{id}/deactivate")
    public ZonaSeguraResponse inactivar(@PathVariable UUID id) {
        return zonaSeguraService.inactivar(id);
    }

    @Operation(
        summary = "Reactivar zona segura",
        description = "Solo ADMIN. Se rechaza con 422 si la zona ya está activa."
    )
    @PatchMapping("/{id}/reactivate")
    public ZonaSeguraResponse reactivar(@PathVariable UUID id) {
        return zonaSeguraService.reactivar(id);
    }

    @Operation(
        summary = "Listar zonas seguras",
        description = "Cualquier rol."
    )
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping
    public List<ZonaSeguraResponse> listar() {
        return zonaSeguraService.listar();
    }

    @Operation(
        summary = "Consultar zona segura",
        description = "Cualquier rol."
    )
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/{id}")
    public ZonaSeguraResponse obtener(@PathVariable UUID id) {
        return zonaSeguraService.obtener(id);
    }
}