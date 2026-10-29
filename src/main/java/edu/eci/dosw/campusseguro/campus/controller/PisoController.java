package edu.eci.dosw.campusseguro.campus.controller;

import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.PisoRequest;
import edu.eci.dosw.campusseguro.campus.dto.PisoResponse;
import edu.eci.dosw.campusseguro.campus.service.PisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// RF-02: los pisos se crean y listan desde su edificio; el resto de operaciones van por id del piso.
@Tag(name = "Pisos", description = "RF-02. Pisos de un edificio, identificados por número (negativo para sótanos).")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PisoController {

    private final PisoService pisoService;

    @Operation(summary = "Registrar piso", description = "Solo ADMIN. El edificio debe estar activo y el número no se puede repetir en el edificio.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PostMapping("/buildings/{edificioId}/floors")
    public ResponseEntity<PisoResponse> crear(@PathVariable UUID edificioId,
                                              @Valid @RequestBody PisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pisoService.crear(edificioId, request));
    }

    @Operation(summary = "Listar pisos de un edificio", description = "Cualquier rol. Ordenados por número. Por defecto solo los activos; incluirInactivos=true devuelve todos.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/buildings/{edificioId}/floors")
    public List<PisoResponse> listarPorEdificio(@PathVariable UUID edificioId,
                                                @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return pisoService.listarPorEdificio(edificioId, incluirInactivos);
    }

    @Operation(summary = "Consultar piso", description = "Cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/floors/{id}")
    public PisoResponse obtener(@PathVariable UUID id) {
        return pisoService.obtener(id);
    }

    @Operation(summary = "Modificar piso", description = "Solo ADMIN. Cambia número y nombre; el número no se puede repetir en el edificio.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PutMapping("/floors/{id}")
    public PisoResponse modificar(@PathVariable UUID id,
                                  @Valid @RequestBody PisoRequest request) {
        return pisoService.modificar(id, request);
    }

    @Operation(summary = "Inactivar piso", description = "Solo ADMIN. Se rechaza con 422 si el piso tiene espacios activos.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/floors/{id}/deactivate")
    public PisoResponse inactivar(@PathVariable UUID id) {
        return pisoService.inactivar(id);
    }

    @Operation(summary = "Reactivar piso", description = "Solo ADMIN. El edificio debe estar activo; sus espacios conservan su estado.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/floors/{id}/reactivate")
    public PisoResponse reactivar(@PathVariable UUID id) {
        return pisoService.reactivar(id);
    }
}
