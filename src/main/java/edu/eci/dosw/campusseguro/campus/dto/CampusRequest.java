package edu.eci.dosw.campusseguro.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Se usa para crear y para modificar un campus.
public record CampusRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        String descripcion
) {
}
