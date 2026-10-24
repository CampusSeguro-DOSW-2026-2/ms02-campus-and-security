package edu.eci.dosw.campusseguro.campus.dto;

import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModificarEspacioRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "El tipo de espacio es obligatorio")
        TipoEspacio tipo
) {
}
