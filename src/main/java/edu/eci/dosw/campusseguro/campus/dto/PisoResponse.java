package edu.eci.dosw.campusseguro.campus.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record PisoResponse(
        UUID id,
        UUID edificioId,
        Integer numero,
        String nombre,
        Estado estado
) {
}
