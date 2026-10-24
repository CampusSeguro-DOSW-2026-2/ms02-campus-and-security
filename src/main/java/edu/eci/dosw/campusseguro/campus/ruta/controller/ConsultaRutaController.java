package edu.eci.dosw.campusseguro.campus.ruta.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.ConsultaRutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Consulta de rutas", description = "RF-06. Ruta recomendada para el usuario según el edificio o el espacio donde está.")
@RestController
public class ConsultaRutaController {

    private final ConsultaRutaService consultaService;

    public ConsultaRutaController(ConsultaRutaService consultaService) {
        this.consultaService = consultaService;
    }

    @Operation(summary = "Ruta de un espacio", description = "Cualquier rol. Devuelve la ruta con su estado y motivo, o el aviso de que el espacio no tiene ruta. 422 si el espacio no está disponible o su edificio está inactivo.")
    @GetMapping("/api/v1/spaces/{spaceId}/evacuation-route")
    public RutaConsultadaResponse porEspacio(@PathVariable UUID spaceId) {
        return consultaService.consultarPorEspacio(spaceId);
    }

    @Operation(summary = "Rutas de un edificio", description = "Cualquier rol. Lista los espacios disponibles del edificio, cada uno con su ruta o el aviso de que no tiene. 422 si el edificio está inactivo.")
    @GetMapping("/api/v1/buildings/{buildingId}/evacuation-routes")
    public List<RutaConsultadaResponse> porEdificio(@PathVariable UUID buildingId) {
        return consultaService.consultarPorEdificio(buildingId);
    }
}
