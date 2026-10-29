package edu.eci.dosw.campusseguro.campus.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record ZonaSeguraResponse(
        UUID id,
        String codigo,
        String nombre,
        String ubicacion,
        String descripcion,
        Integer capacidad,
        Estado estado
) {
}