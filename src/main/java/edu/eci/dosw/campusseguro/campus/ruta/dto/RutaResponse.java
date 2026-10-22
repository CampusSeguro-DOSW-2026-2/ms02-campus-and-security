package edu.eci.dosw.campusseguro.campus.ruta.dto;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;

import java.time.Instant;

public record RutaResponse(
        Long id,
        Long espacioOrigenId,
        Long zonaSeguraDestinoId,
        Long rutaAlternativaId,
        String descripcion,
        EstadoRuta estado,
        Instant actualizadaEn) {

    public static RutaResponse desde(RutaEvacuacion ruta) {
        return new RutaResponse(
                ruta.getId(),
                ruta.getEspacioOrigenId(),
                ruta.getZonaSeguraDestinoId(),
                ruta.getRutaAlternativaId(),
                ruta.getDescripcion(),
                ruta.getEstado(),
                ruta.getUpdatedAt());
    }
}
