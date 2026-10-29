package edu.eci.dosw.campusseguro.campus.ruta.dto;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoRuta estado,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo) {
}
