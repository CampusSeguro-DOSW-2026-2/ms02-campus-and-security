package edu.eci.dosw.campusseguro.campus.ruta.dto;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoRuta estado) {
}
