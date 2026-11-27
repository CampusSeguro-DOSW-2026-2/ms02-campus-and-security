package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoZonaAbierta;

public record ZonaAbiertaResponse(
        UUID id,
        UUID campusId,
        String nombre,
        TipoZonaAbierta tipo,
        BigDecimal latitud,
        BigDecimal longitud,
        Estado estado
) {
}
