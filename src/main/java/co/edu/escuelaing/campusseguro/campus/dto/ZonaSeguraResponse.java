package co.edu.escuelaing.campusseguro.campus.dto;

import co.edu.escuelaing.campusseguro.campus.entity.Estado;

public record ZonaSeguraResponse(
        Long id,
        String nombre,
        String ubicacion,
        String descripcion,
        Integer capacidad,
        Estado estado
) {
}