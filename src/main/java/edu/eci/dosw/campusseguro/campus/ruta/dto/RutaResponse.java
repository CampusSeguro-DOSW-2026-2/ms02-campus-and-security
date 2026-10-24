package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;

import java.time.Instant;

public record RutaResponse(
        UUID id,
        UUID espacioOrigenId,
        UUID zonaSeguraDestinoId,
        UUID rutaAlternativaId,
        String descripcion,
        EstadoRuta estado,
        String motivo,
        Instant actualizadaEn) {

    public static RutaResponse desde(RutaEvacuacion ruta) {
        return new RutaResponse(
                ruta.getId(),
                ruta.getEspacioOrigenId(),
                ruta.getZonaSeguraDestinoId(),
                ruta.getRutaAlternativaId(),
                ruta.getDescripcion(),
                ruta.getEstado(),
                ruta.getMotivo(),
                ruta.getUpdatedAt());
    }
}
