package co.edu.escuelaing.campusseguro.campus.ruta.dto;

import co.edu.escuelaing.campusseguro.campus.ruta.domain.EstadoRuta;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoRuta estado) {
}
