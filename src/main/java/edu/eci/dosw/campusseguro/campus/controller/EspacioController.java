package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEspacioRequest;
import edu.eci.dosw.campusseguro.campus.service.EspacioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Espacios", description = "RF-03. Salones, laboratorios, auditorios, oficinas y espacios comunes de un piso.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EspacioController {

    private final EspacioService espacioService;

    @Operation(summary = "Registrar espacio", description = "Solo ADMIN. El piso y su edificio deben estar activos y el código no se puede repetir en el edificio.")
    @PostMapping("/floors/{pisoId}/spaces")
    public ResponseEntity<EspacioResponse> crear(@PathVariable UUID pisoId,
                                                 @Valid @RequestBody EspacioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crear(pisoId, request));
    }

    @Operation(summary = "Listar espacios de un piso", description = "Cualquier rol.")
    @GetMapping("/floors/{pisoId}/spaces")
    public List<EspacioResponse> listarPorPiso(@PathVariable UUID pisoId) {
        return espacioService.listarPorPiso(pisoId);
    }

    @Operation(summary = "Listar espacios de un edificio", description = "Cualquier rol.")
    @GetMapping("/buildings/{edificioId}/spaces")
    public List<EspacioResponse> listarPorEdificio(@PathVariable UUID edificioId) {
        return espacioService.listarPorEdificio(edificioId);
    }

    @Operation(summary = "Consultar espacio", description = "Cualquier rol.")
    @GetMapping("/spaces/{id}")
    public EspacioResponse obtener(@PathVariable UUID id) {
        return espacioService.obtener(id);
    }

    @Operation(summary = "Modificar espacio", description = "Solo ADMIN. Cambia nombre y tipo; el código no se modifica.")
    @PutMapping("/spaces/{id}")
    public EspacioResponse modificar(@PathVariable UUID id,
                                     @Valid @RequestBody ModificarEspacioRequest request) {
        return espacioService.modificar(id, request);
    }

    @Operation(summary = "Inactivar espacio",
            description = "Solo ADMIN. Se rechaza con 422 si el espacio es origen de una ruta Disponible o Restringida.")
    @PatchMapping("/spaces/{id}/deactivate")
    public EspacioResponse inactivar(@PathVariable UUID id) {
        return espacioService.inactivar(id);
    }
}
