package edu.eci.dosw.campusseguro.campus.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.CampusRequest;
import edu.eci.dosw.campusseguro.campus.dto.CampusResponse;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import edu.eci.dosw.campusseguro.campus.service.CampusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Campus", description = "ADR-003. Gestión de campus; agrupa edificios y zonas abiertas.")
@RestController
@RequestMapping("/api/v1/campuses")
@RequiredArgsConstructor
public class CampusController {

    private final CampusService campusService;

    @Operation(
            summary = "Crear campus",
            description = "Solo ADMIN. El campus queda activo.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PostMapping
    public ResponseEntity<CampusResponse> crear(@Valid @RequestBody CampusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campusService.crear(request));
    }

    @Operation(
            summary = "Modificar campus",
            description = "Solo ADMIN. Cambia nombre y descripción.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PutMapping("/{id}")
    public CampusResponse modificar(@PathVariable UUID id,
                                    @Valid @RequestBody CampusRequest request) {
        return campusService.modificar(id, request);
    }

    @Operation(
            summary = "Inactivar campus",
            description = "Solo ADMIN. Se rechaza con 422 si el campus tiene edificios o zonas abiertas activas.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/{id}/deactivate")
    public CampusResponse inactivar(@PathVariable UUID id) {
        return campusService.inactivar(id);
    }

    @Operation(
            summary = "Reactivar campus",
            description = "Solo ADMIN. Sus edificios y zonas abiertas conservan su estado; se reactivan por separado.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/{id}/reactivate")
    public CampusResponse reactivar(@PathVariable UUID id) {
        return campusService.reactivar(id);
    }

    @Operation(
            summary = "Listar campus",
            description = "Cualquier rol. Por defecto solo los activos; incluirInactivos=true devuelve todos.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping
    public List<CampusResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return campusService.listar(incluirInactivos);
    }

    @Operation(summary = "Consultar campus", description = "Cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/{id}")
    public CampusResponse obtener(@PathVariable UUID id) {
        return campusService.obtener(id);
    }
}
