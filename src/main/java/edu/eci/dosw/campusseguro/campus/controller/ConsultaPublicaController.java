package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioPublicoResponse;
import edu.eci.dosw.campusseguro.campus.dto.SalidaPublicaResponse;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraPublicaResponse;
import edu.eci.dosw.campusseguro.campus.service.ConsultaPublicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Consulta pública", description = "ADR-003 (D7). Lo que se ve al escanear un QR en una emergencia: solo lectura, sin sesión y con datos mínimos.")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class ConsultaPublicaController {

    private final ConsultaPublicaService consultaPublicaService;

    @Operation(summary = "Consultar un espacio por su QR", description = "Sin sesión. Edificio, piso y la evacuación recomendada; si el espacio no está disponible devuelve un aviso.")
    @GetMapping("/spaces")
    public EspacioPublicoResponse espacio(@RequestParam UUID id) {
        return consultaPublicaService.espacio(id);
    }

    @Operation(summary = "Consultar una salida por su QR", description = "Sin sesión. Edificio, piso y coordenadas de la salida.")
    @GetMapping("/exits")
    public SalidaPublicaResponse salida(@RequestParam UUID id) {
        return consultaPublicaService.salida(id);
    }

    @Operation(summary = "Consultar una zona segura por su QR", description = "Sin sesión. Ubicación, coordenadas y si es punto de encuentro.")
    @GetMapping("/safe-zones")
    public ZonaSeguraPublicaResponse zonaSegura(@RequestParam UUID id) {
        return consultaPublicaService.zonaSegura(id);
    }
}
