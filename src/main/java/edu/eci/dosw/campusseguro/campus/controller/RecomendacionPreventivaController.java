package edu.eci.dosw.campusseguro.campus.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaRequest;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import edu.eci.dosw.campusseguro.campus.service.RecomendacionPreventivaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recomendaciones preventivas", description = "RF-13. Información preventiva para edificios y tipos de espacio.")
@RestController
@RequestMapping("/api/v1/preventive-recommendations")
@RequiredArgsConstructor
public class RecomendacionPreventivaController {

    private final RecomendacionPreventivaService service;

    @Operation(summary = "Listar recomendaciones activas", description = "Consulta disponible para cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping
    public List<RecomendacionPreventivaResponse> listar() {
        return service.listarActivas();
    }

    @Operation(summary = "Consultar recomendaciones de un edificio", description = "Consulta disponible para cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/building/{buildingId}")
    public List<RecomendacionPreventivaResponse> porEdificio(@PathVariable UUID buildingId) {
        return service.consultarPorEdificio(buildingId);
    }

    @Operation(summary = "Consultar recomendaciones por tipo de espacio", description = "Consulta disponible para cualquier rol.")
    @Permiso(Permisos.CAMPUS_READ)
    @GetMapping("/space-type/{type}")
    public List<RecomendacionPreventivaResponse> porTipo(@PathVariable TipoEspacio type) {
        return service.consultarPorTipo(type);
    }

    @Operation(summary = "Crear recomendación preventiva", description = "Solo ADMIN.")
    @Permiso(Permisos.PREVENTIVE_RECOMMENDATION_WRITE)
    @PostMapping
    public ResponseEntity<RecomendacionPreventivaResponse> crear(
            @Valid @RequestBody RecomendacionPreventivaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Modificar recomendación preventiva", description = "Solo ADMIN.")
    @Permiso(Permisos.PREVENTIVE_RECOMMENDATION_WRITE)
    @PutMapping("/{id}")
    public RecomendacionPreventivaResponse modificar(
            @PathVariable UUID id,
            @Valid @RequestBody RecomendacionPreventivaRequest request) {
        return service.modificar(id, request);
    }

    @Operation(summary = "Inactivar recomendación preventiva", description = "Solo ADMIN.")
    @Permiso(Permisos.PREVENTIVE_RECOMMENDATION_WRITE)
    @PatchMapping("/{id}/deactivate")
    public RecomendacionPreventivaResponse inactivar(@PathVariable UUID id) {
        return service.inactivar(id);
    }
}
