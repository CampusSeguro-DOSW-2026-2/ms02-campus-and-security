package edu.eci.dosw.campusseguro.campus.ruta.dto;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;


public record RutaConsultadaResponse(
        Long espacioId,
        String espacioNombre,
        boolean tieneRuta,
        Long zonaSeguraId,
        String zonaSeguraNombre,
        String zonaSeguraUbicacion,
        String recorrido,
        EstadoRuta estado,
        boolean destinoActivo,
        String mensaje) {
}
