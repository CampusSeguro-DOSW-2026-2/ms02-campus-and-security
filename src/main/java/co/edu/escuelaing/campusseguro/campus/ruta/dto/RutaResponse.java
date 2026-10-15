package co.edu.escuelaing.campusseguro.campus.ruta.dto;

import co.edu.escuelaing.campusseguro.campus.ruta.domain.EstadoRuta;
import co.edu.escuelaing.campusseguro.campus.ruta.domain.RutaEvacuacion;

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
