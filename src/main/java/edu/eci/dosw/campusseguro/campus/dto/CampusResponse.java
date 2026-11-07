package edu.eci.dosw.campusseguro.campus.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record CampusResponse(
        UUID id,
        String nombre,
        String descripcion,
        Estado estado
) {
}
