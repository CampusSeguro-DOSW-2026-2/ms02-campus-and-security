package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CalcularRutaExteriorRequest(
        @NotNull(message = "La salida de origen es obligatoria")
        UUID salidaId,

        @NotNull(message = "La zona segura de destino es obligatoria")
        UUID zonaSeguraId) {
}
