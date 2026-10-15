package co.edu.escuelaing.campusseguro.campus.ruta.dto;

import co.edu.escuelaing.campusseguro.campus.ruta.domain.EstadoRuta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CrearRutaRequest(
        @NotNull(message = "El espacio de origen es obligatorio")
        Long espacioOrigenId,

        @NotNull(message = "La zona segura de destino es obligatoria")
        Long zonaSeguraDestinoId,

        @NotBlank(message = "La descripción de la ruta es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,

        @NotNull(message = "El estado es obligatorio")
        EstadoRuta estado,

        Long rutaAlternativaId) {
}
