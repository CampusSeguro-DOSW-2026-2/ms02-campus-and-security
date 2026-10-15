package co.edu.escuelaing.campusseguro.campus.dto;

import co.edu.escuelaing.campusseguro.campus.entity.Estado;

public record EdificioResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Estado estado
) {
}