package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record CrearRutaRequest(
        @NotNull(message = "El espacio de origen es obligatorio")
        UUID espacioOrigenId,

        @NotNull(message = "La zona segura de destino es obligatoria")
        UUID zonaSeguraDestinoId,

        @NotBlank(message = "La descripción de la ruta es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,

        @NotNull(message = "El estado es obligatorio")
        EstadoRuta estado,

        @Size(max = 255, message = "El motivo no puede superar 255 caracteres")
        String motivo,

        UUID rutaAlternativaId) {
}
