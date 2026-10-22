package edu.eci.dosw.campusseguro.campus.dto;

import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record ZonaSeguraResponse(
        Long id,
        String nombre,
        String ubicacion,
        String descripcion,
        Integer capacidad,
        Estado estado
) {
}