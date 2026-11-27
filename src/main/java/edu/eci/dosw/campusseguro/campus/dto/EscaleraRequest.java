package edu.eci.dosw.campusseguro.campus.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record EscaleraRequest(
        @NotBlank(message = "El nombre de la escalera es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotEmpty(message = "Debe seleccionar los pisos que conecta la escalera")
        @Size(min = 2, message = "Una escalera debe conectar al menos dos pisos")
        List<UUID> pisoIds,

        @Min(value = 1, message = "La prioridad debe ser 1 o mayor")
        Integer prioridad
) {
}
