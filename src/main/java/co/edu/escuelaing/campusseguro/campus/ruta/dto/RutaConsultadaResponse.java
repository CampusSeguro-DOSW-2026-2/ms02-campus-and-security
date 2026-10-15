package co.edu.escuelaing.campusseguro.campus.ruta.dto;

import co.edu.escuelaing.campusseguro.campus.ruta.domain.EstadoRuta;


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
