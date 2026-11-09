package edu.eci.dosw.campusseguro.campus.ruta.controller;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.dto.CalcularRutaExteriorRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.ResumenGeneracionResponse;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaExteriorResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.GeneradorRutasExterioresService;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaExteriorService;
import edu.eci.dosw.campusseguro.campus.security.Permiso;
import edu.eci.dosw.campusseguro.campus.security.Permisos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Rutas exteriores",
        description = "ADR-003. Recorrido peatonal desde una salida del edificio hasta una zona segura."
)
@RestController
public class RutaExteriorController {

    private final RutaExteriorService rutaExteriorService;
    private final GeneradorRutasExterioresService generador;

    public RutaExteriorController(RutaExteriorService rutaExteriorService,
                                  GeneradorRutasExterioresService generador) {
        this.rutaExteriorService = rutaExteriorService;
        this.generador = generador;
    }

    @Operation(
            summary = "Calcular ruta exterior",
            description = "Solo ADMIN. Calcula el recorrido peatonal salida -> zona segura con el proveedor de navegación "
                    + "y guarda el último resultado. Si el par ya tenía ruta, la recalcula. "
                    + "La salida y la zona deben estar activas y con coordenadas."
    )
    @ApiResponse(responseCode = "422",
            description = "La salida o la zona no cumplen las reglas de negocio, o no hay recorrido peatonal.")
    @ApiResponse(responseCode = "404", description = "No existe la salida o la zona segura.")
    @ApiResponse(responseCode = "503", description = "El proveedor de navegación no está disponible.")
    @Permiso(Permisos.ROUTE_WRITE)
    @PostMapping("/api/v1/evacuation-routes/exterior")
    public RutaExteriorResponse calcular(@Valid @RequestBody CalcularRutaExteriorRequest request) {
        return rutaExteriorService.calcular(request.salidaId(), request.zonaSeguraId());
    }

    @Operation(
            summary = "Generar rutas exteriores del campus",
            description = "Solo ADMIN. Conecta cada salida disponible con sus zonas seguras más cercanas. "
                    + "Por defecto solo crea las rutas que faltan; con recalcular=true rehace también las existentes. "
                    + "Normalmente no hace falta: las rutas se generan solas al crear o mover salidas y zonas."
    )
    @ApiResponse(responseCode = "200",
            description = "Resumen; interrumpida=true si el proveedor de navegación dejó de responder (repetir luego).")
    @Permiso(Permisos.ROUTE_WRITE)
    @PostMapping("/api/v1/evacuation-routes/exterior/generate")
    public ResumenGeneracionResponse generar(
            @Parameter(description = "Si es true, recalcula también las rutas que ya existen")
            @RequestParam(defaultValue = "false") boolean recalcular) {
        return generador.generarTodas(recalcular);
    }

    @Operation(
            summary = "Rutas exteriores de una salida",
            description = "Cualquier rol. Devuelve los últimos recorridos guardados desde la salida, sin llamar al proveedor."
    )
    @Permiso(Permisos.ROUTE_READ)
    @GetMapping("/api/v1/exits/{exitId}/evacuation-route")
    public List<RutaExteriorResponse> porSalida(
            @Parameter(description = "Identificador de la salida", schema = @Schema(type = "string", format = "uuid"))
            @PathVariable UUID exitId) {
        return rutaExteriorService.listarPorSalida(exitId);
    }
}
