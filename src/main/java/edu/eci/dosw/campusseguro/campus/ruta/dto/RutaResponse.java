package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;

import java.time.Instant;

public record RutaResponse(
        UUID id,
        UUID espacioOrigenId,
        UUID zonaSeguraDestinoId,
        String descripcion,
        EstadoRuta estado,
        Instant actualizadaEn) {

    public static RutaResponse desde(RutaEvacuacion ruta) {
        return new RutaResponse(
                ruta.getId(),
                ruta.getEspacioOrigenId(),
                ruta.getZonaSeguraDestinoId(),
                ruta.getDescripcion(),
                ruta.getEstado(),
                ruta.getUpdatedAt());
    }
}
