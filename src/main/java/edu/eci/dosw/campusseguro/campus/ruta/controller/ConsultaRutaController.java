package edu.eci.dosw.campusseguro.campus.ruta.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.ConsultaRutaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Consulta de rutas",
        description = "RF-06. Ruta recomendada para el usuario según el edificio o el espacio donde está."
)
@RestController
public class ConsultaRutaController {

    private final ConsultaRutaService consultaService;

    public ConsultaRutaController(ConsultaRutaService consultaService) {
        this.consultaService = consultaService;
    }

    @Operation(
            summary = "Ruta de un espacio",
            description = "Cualquier rol. Devuelve la ruta con su estado o el aviso de que el espacio no tiene ruta."
    )
    @ApiResponse(
            responseCode = "422",
            description = "El espacio consultado está inactivo."
    )
    @GetMapping("/api/v1/spaces/{spaceId}/evacuation-route")
    public RutaConsultadaResponse porEspacio(
            @Parameter(
                    description = "Identificador del espacio",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID spaceId) {

        return consultaService.consultarPorEspacio(spaceId);
    }

    @Operation(
            summary = "Rutas de un edificio",
            description = "Cualquier rol. Lista los espacios activos del edificio, cada uno con su ruta o el aviso de que no tiene. 422 si el edificio está inactivo."
    )
    @ApiResponse(
            responseCode = "422",
            description = "El edificio consultado está inactivo."
    )
    @GetMapping("/api/v1/buildings/{buildingId}/evacuation-routes")
    public List<RutaConsultadaResponse> porEdificio(
            @Parameter(
                    description = "Identificador del edificio",
                    schema = @Schema(type = "string", format = "uuid")
            )
            @PathVariable UUID buildingId) {

        return consultaService.consultarPorEdificio(buildingId);
    }
}
