package co.edu.escuelaing.campusseguro.campus.ruta.controller;

import co.edu.escuelaing.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import co.edu.escuelaing.campusseguro.campus.ruta.service.ConsultaRutaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ConsultaRutaController {

    private final ConsultaRutaService consultaService;

    public ConsultaRutaController(ConsultaRutaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping("/api/v1/spaces/{spaceId}/evacuation-route")
    public RutaConsultadaResponse porEspacio(@PathVariable Long spaceId) {
        return consultaService.consultarPorEspacio(spaceId);
    }

    @GetMapping("/api/v1/buildings/{buildingId}/evacuation-routes")
    public List<RutaConsultadaResponse> porEdificio(@PathVariable Long buildingId) {
        return consultaService.consultarPorEdificio(buildingId);
    }
}
