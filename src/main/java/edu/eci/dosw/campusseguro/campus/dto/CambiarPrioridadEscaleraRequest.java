package edu.eci.dosw.campusseguro.campus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CambiarPrioridadEscaleraRequest(
        @NotNull(message = "La prioridad es obligatoria")
        @Min(value = 1, message = "La prioridad debe ser 1 o mayor")
        Integer prioridad
) {
}
