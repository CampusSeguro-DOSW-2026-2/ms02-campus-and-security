package edu.eci.dosw.campusseguro.campus.dto;

import edu.eci.dosw.campusseguro.campus.entity.DisponibilidadEscalera;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarDisponibilidadEscaleraRequest(
        @NotNull(message = "La disponibilidad es obligatoria")
        DisponibilidadEscalera disponibilidad,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo
) {
}
