package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;

import edu.eci.dosw.campusseguro.campus.entity.TipoZonaAbierta;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Se usa para crear y para modificar una zona abierta. Latitud y longitud son opcionales, pero van juntas.
public record ZonaAbiertaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "El tipo es obligatorio")
        TipoZonaAbierta tipo,

        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        BigDecimal latitud,

        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        BigDecimal longitud
) {
}
