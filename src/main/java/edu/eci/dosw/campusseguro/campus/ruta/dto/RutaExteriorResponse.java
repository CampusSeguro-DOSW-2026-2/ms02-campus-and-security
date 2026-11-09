package edu.eci.dosw.campusseguro.campus.ruta.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.PasoRuta;

// Último recorrido exterior calculado entre una salida y una zona segura.
public record RutaExteriorResponse(
        UUID id,
        UUID salidaOrigenId,
        UUID zonaSeguraDestinoId,
        EstadoRuta estado,
        String motivo,
        BigDecimal distanciaMetros,
        BigDecimal duracionSegundos,
        List<Coordenada> geometria,
        List<PasoRuta> pasos,
        Instant calculadaEn) {
}
