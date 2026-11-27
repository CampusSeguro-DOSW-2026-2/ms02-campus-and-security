package edu.eci.dosw.campusseguro.campus.dto;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.DisponibilidadEscalera;
import edu.eci.dosw.campusseguro.campus.entity.Estado;

public record EscaleraResponse(
        UUID id,
        UUID edificioId,
        String codigo,
        String nombre,
        DisponibilidadEscalera disponibilidad,
        String motivo,
        Estado estado,
        int prioridad,
        List<UUID> pisoIds
) {
}
