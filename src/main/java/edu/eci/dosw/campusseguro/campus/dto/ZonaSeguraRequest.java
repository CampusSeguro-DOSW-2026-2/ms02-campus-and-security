package edu.eci.dosw.campusseguro.campus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ZonaSeguraRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "La ubicación es obligatoria")
        @Size(max = 255, message = "La ubicación no puede superar 255 caracteres")
        String ubicacion,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser mayor que 0")
        Integer capacidad
) {
}