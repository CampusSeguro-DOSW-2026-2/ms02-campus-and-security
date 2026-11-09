package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;
import java.util.List;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;

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
        String motivo,
        boolean destinoActivo,
        String mensaje,
        List<RecomendacionPreventivaResponse> recomendacionesPreventivas,
        GuiaEvacuacionResponse guia) {

    // Constructor previo a la guía: los consumidores existentes no cambian.
    public RutaConsultadaResponse(
            UUID espacioId, String espacioNombre, boolean tieneRuta,
            UUID zonaSeguraId, String zonaSeguraNombre, String zonaSeguraUbicacion,
            String recorrido, EstadoRuta estado, String motivo,
            boolean destinoActivo, String mensaje,
            List<RecomendacionPreventivaResponse> recomendacionesPreventivas) {
        this(espacioId, espacioNombre, tieneRuta, zonaSeguraId, zonaSeguraNombre, zonaSeguraUbicacion,
                recorrido, estado, motivo, destinoActivo, mensaje, recomendacionesPreventivas, null);
    }

    public RutaConsultadaResponse(
            UUID espacioId, String espacioNombre, boolean tieneRuta,
            UUID zonaSeguraId, String zonaSeguraNombre, String zonaSeguraUbicacion,
            String recorrido, EstadoRuta estado, String motivo,
            boolean destinoActivo, String mensaje) {
        this(espacioId, espacioNombre, tieneRuta, zonaSeguraId, zonaSeguraNombre,
                zonaSeguraUbicacion, recorrido, estado, motivo, destinoActivo,
                mensaje, List.of(), null);
    }
}
