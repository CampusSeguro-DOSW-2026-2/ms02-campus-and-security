package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EdificioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEdificioRequest;
import edu.eci.dosw.campusseguro.campus.service.EdificioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Edificios", description = "RF-01. Gestión de edificios del campus.")
@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
public class EdificioController {

    private final EdificioService edificioService;

    @Operation(summary = "Crear edificio", description = "Solo ADMIN. El código es único. El edificio queda activo.")
    @PostMapping
    public ResponseEntity<EdificioResponse> crear(@Valid @RequestBody EdificioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(edificioService.crear(request));
    }

    @Operation(summary = "Modificar edificio", description = "Solo ADMIN. Cambia código, nombre y descripción.")
    @PutMapping("/{id}")
    public EdificioResponse modificar(@PathVariable UUID id,
                                  @Valid @RequestBody ModificarEdificioRequest request) {
        return edificioService.modificar(id, request);
    }

    @Operation(summary = "Inactivar edificio", description = "Solo ADMIN. Cambia el estado a inactivo sin eliminarlo.")
    @PatchMapping("/{id}/deactivate")
    public EdificioResponse inactivar(@PathVariable UUID id) {
        return edificioService.inactivar(id);
    }

    @Operation(summary = "Listar edificios", description = "Cualquier rol.")
    @GetMapping
    public List<EdificioResponse> listar() {
        return edificioService.listar();
    }

    @Operation(summary = "Consultar edificio", description = "Cualquier rol.")
    @GetMapping("/{id}")
    public EdificioResponse obtener(@PathVariable UUID id) {
        return edificioService.obtener(id);
    }
}