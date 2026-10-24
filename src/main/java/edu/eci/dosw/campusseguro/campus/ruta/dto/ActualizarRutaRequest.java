package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarRutaRequest(
        @NotNull(message = "La zona segura de destino es obligatoria")
        UUID zonaSeguraDestinoId,

        @NotBlank(message = "La descripción de la ruta es obligatoria")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion) {
}
