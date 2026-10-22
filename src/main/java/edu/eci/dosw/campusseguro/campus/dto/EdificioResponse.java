package edu.eci.dosw.campusseguro.campus.dto;

import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record EdificioResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Estado estado
) {
}