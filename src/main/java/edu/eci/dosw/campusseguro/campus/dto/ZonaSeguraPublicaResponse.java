package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;

public record ZonaSeguraPublicaResponse(
        String codigo,
        String nombre,
        String ubicacion,
        boolean puntoEncuentro,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean disponible,
        String aviso,
        boolean tieneFotografia
) {
}
