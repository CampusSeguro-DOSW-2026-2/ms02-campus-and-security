package edu.eci.dosw.campusseguro.campus.ruta.controller;

import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.dto.CambiarEstadoRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Rutas de evacuación",
        description = "RF-MS02-05. Las rutas de evacuación se generan solas (ver /evacuation-routes/exterior); "
                + "aquí solo se consultan y se cambia su estado."
)
@RestController
@RequestMapping("/api/v1/evacuation-routes")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @Operation(
            summary = "Cambiar estado de una ruta",
            description = "ADMIN o COORDINADOR. BLOQUEADA y RESTRINGIDA exigen motivo; "
                    + "al pasar a DISPONIBLE el motivo se borra. "
                    + "DISPONIBLE y RESTRINGIDA exigen espacio de origen disponible "
                    + "y zona de destino activa."
    )
    @ApiResponse(
            responseCode = "422",
            description = "El espacio de origen o la zona segura de destino no está habilitado para el estado solicitado."
    )
    @Permiso(Permisos.ROUTE_STATUS_WRITE)
    @PatchMapping("/{id}/status")
    public RutaResponse cambiarEstado(
            @Parameter(
                    description = "Identificador de la ruta",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoRequest request) {

        return rutaService.cambiarEstado(
                id,
                request.estado(),
                request.motivo()
        );
    }

    @Operation(
            summary = "Consultar ruta",
            description = "Cualquier rol."
    )
    @Permiso(Permisos.ROUTE_READ)
    @GetMapping("/{id}")
    public RutaResponse obtener(
            @Parameter(
                    description = "Identificador de la ruta",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID id) {

        return rutaService.obtener(id);
    }

    @Operation(
            summary = "Listar rutas",
            description = "Cualquier rol."
    )
    @Permiso(Permisos.ROUTE_READ)
    @GetMapping
    public List<RutaResponse> listar() {
        return rutaService.listar();
    }
}
