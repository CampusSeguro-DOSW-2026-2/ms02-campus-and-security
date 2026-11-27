package edu.eci.dosw.campusseguro.campus.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModificarEdificioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        String descripcion,

        // Opcional: si no se envía, el campus actual no cambia.
        UUID campusId
) {
}