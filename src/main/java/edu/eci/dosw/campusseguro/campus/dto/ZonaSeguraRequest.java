package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ZonaSeguraRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El código no puede superar 50 caracteres")
        String codigo,

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
        Integer capacidad,

        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        BigDecimal latitud,

        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        BigDecimal longitud,

        // Opcional: zona abierta donde está ubicada la zona segura.
        UUID zonaAbiertaId,

        // Opcional: si no se envía, al crear queda como punto de encuentro y al modificar no cambia.
        Boolean puntoEncuentro
) {
}