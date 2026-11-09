package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Guía completa desde un espacio: escalera (si hace falta), salida y ruta exterior hasta la zona segura.
public record GuiaEvacuacionResponse(
        UUID edificioId,
        String edificioNombre,
        UUID pisoId,
        Integer pisoNumero,
        boolean requiereEscalera,
        List<EscaleraGuia> escaleras,
        SalidaGuia salida,
        RutaExteriorResponse rutaExterior,
        String zonaSeguraNombre,
        String indicacion,
        String mensaje) {

    public record EscaleraGuia(UUID id, String codigo, String nombre) {
    }

    public record SalidaGuia(UUID id, String nombre, BigDecimal latitud, BigDecimal longitud) {
    }
}
