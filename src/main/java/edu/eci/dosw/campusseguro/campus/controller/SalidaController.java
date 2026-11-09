package edu.eci.dosw.campusseguro.campus.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ModificarSalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaResponse;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import edu.eci.dosw.campusseguro.campus.service.SalidaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Salidas", description = "ADR-003. Salidas del edificio al exterior, con el piso desde el que se sale y sus coordenadas.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SalidaController {

    private final SalidaService salidaService;

    @Operation(summary = "Registrar salida", description = "Solo ADMIN. El edificio y el piso deben estar activos, el piso debe ser del edificio y las coordenadas son obligatorias.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PostMapping("/buildings/{edificioId}/exits")
    public ResponseEntity<SalidaResponse> crear(@PathVariable UUID edificioId,
                                                @Valid @RequestBody SalidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salidaService.crear(edificioId, request));
    }

    @Operation(summary = "Listar salidas de un edificio", description = "Cualquier rol. Por defecto solo las disponibles; incluirInactivos=true devuelve todas.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/buildings/{edificioId}/exits")
    public List<SalidaResponse> listarPorEdificio(@PathVariable UUID edificioId,
                                                  @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return salidaService.listarPorEdificio(edificioId, incluirInactivos);
    }

    @Operation(summary = "Consultar salida", description = "Cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/exits/{id}")
    public SalidaResponse obtener(@PathVariable UUID id) {
        return salidaService.obtener(id);
    }

    @Operation(summary = "Modificar salida", description = "Solo ADMIN. Cambia piso, nombre y coordenadas.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PutMapping("/exits/{id}")
    public SalidaResponse modificar(@PathVariable UUID id, @Valid @RequestBody ModificarSalidaRequest request) {
        return salidaService.modificar(id, request);
    }

    @Operation(summary = "Inactivar salida", description = "Solo ADMIN. Una salida inactiva no se usa como origen de rutas exteriores.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/exits/{id}/deactivate")
    public SalidaResponse inactivar(@PathVariable UUID id) {
        return salidaService.inactivar(id);
    }

    @Operation(summary = "Reactivar salida", description = "Solo ADMIN. Su piso y su edificio deben estar activos.")
    @Permiso(Permisos.BUILDING_WRITE)
    @PatchMapping("/exits/{id}/reactivate")
    public SalidaResponse reactivar(@PathVariable UUID id) {
        return salidaService.reactivar(id);
    }
}
