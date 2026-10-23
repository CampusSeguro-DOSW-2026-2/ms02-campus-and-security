package edu.eci.dosw.campusseguro.campus.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// El numero puede ser negativo para sotanos.
public record PisoRequest(

        @NotNull(message = "El número del piso es obligatorio")
        Integer numero,

        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre
) {
}
