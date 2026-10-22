package edu.eci.dosw.campusseguro.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EdificioRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El codigo no puede superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        String descripcion
) {
}