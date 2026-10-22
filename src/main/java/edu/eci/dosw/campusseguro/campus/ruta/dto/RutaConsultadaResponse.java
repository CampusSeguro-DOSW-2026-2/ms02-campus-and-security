package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;


public record RutaConsultadaResponse(
        UUID espacioId,
        String espacioNombre,
        boolean tieneRuta,
        UUID zonaSeguraId,
        String zonaSeguraNombre,
        String zonaSeguraUbicacion,
        String recorrido,
        EstadoRuta estado,
        boolean destinoActivo,
        String mensaje) {
}
