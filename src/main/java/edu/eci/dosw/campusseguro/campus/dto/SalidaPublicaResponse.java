package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;

public record SalidaPublicaResponse(
        String codigo,
        String nombre,
        String edificio,
        Integer piso,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean disponible,
        String aviso,
        boolean tieneFotografia
) {
}
