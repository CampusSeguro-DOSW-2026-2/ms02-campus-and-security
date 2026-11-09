package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record SalidaResponse(
        UUID id,
        UUID edificioId,
        String edificioNombre,
        UUID pisoId,
        Integer pisoNumero,
        String codigo,
        String nombre,
        BigDecimal latitud,
        BigDecimal longitud,
        Estado estado,
        boolean disponible,
        boolean tieneFotografia
) {
}
