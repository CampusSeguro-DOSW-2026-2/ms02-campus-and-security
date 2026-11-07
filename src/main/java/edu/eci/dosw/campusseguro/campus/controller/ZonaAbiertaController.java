package edu.eci.dosw.campusseguro.campus.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaResponse;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import edu.eci.dosw.campusseguro.campus.service.ZonaAbiertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Las zonas abiertas se crean y listan desde su campus; el resto de operaciones van por id de la zona.
@Tag(name = "Zonas abiertas", description = "ADR-003. Espacios exteriores del campus (zonas verdes, plazoletas...).")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ZonaAbiertaController {

    private final ZonaAbiertaService zonaAbiertaService;

    @Operation(
            summary = "Registrar zona abierta",
            description = "Solo ADMIN. El campus debe estar activo. "
                    + "Latitud y longitud son opcionales, pero se envían juntas.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PostMapping("/campuses/{campusId}/open-zones")
    public ResponseEntity<ZonaAbiertaResponse> crear(@PathVariable UUID campusId,
                                                     @Valid @RequestBody ZonaAbiertaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaAbiertaService.crear(campusId, request));
    }

    @Operation(
            summary = "Listar zonas abiertas de un campus",
            description = "Cualquier rol. Por defecto solo las activas; incluirInactivos=true devuelve todas.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/campuses/{campusId}/open-zones")
    public List<ZonaAbiertaResponse> listarPorCampus(@PathVariable UUID campusId,
                                                     @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return zonaAbiertaService.listarPorCampus(campusId, incluirInactivos);
    }

    @Operation(summary = "Consultar zona abierta", description = "Cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/open-zones/{id}")
    public ZonaAbiertaResponse obtener(@PathVariable UUID id) {
        return zonaAbiertaService.obtener(id);
    }

    @Operation(
            summary = "Modificar zona abierta",
            description = "Solo ADMIN. Cambia nombre, tipo y coordenadas. Latitud y longitud se envían juntas.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PutMapping("/open-zones/{id}")
    public ZonaAbiertaResponse modificar(@PathVariable UUID id,
                                         @Valid @RequestBody ZonaAbiertaRequest request) {
        return zonaAbiertaService.modificar(id, request);
    }

    @Operation(summary = "Inactivar zona abierta", description = "Solo ADMIN.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/open-zones/{id}/deactivate")
    public ZonaAbiertaResponse inactivar(@PathVariable UUID id) {
        return zonaAbiertaService.inactivar(id);
    }

    @Operation(
            summary = "Reactivar zona abierta",
            description = "Solo ADMIN. Se rechaza con 422 si el campus está inactivo.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/open-zones/{id}/reactivate")
    public ZonaAbiertaResponse reactivar(@PathVariable UUID id) {
        return zonaAbiertaService.reactivar(id);
    }
}
