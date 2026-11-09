package edu.eci.dosw.campusseguro.campus.ruta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

// Punto geográfico exterior (WGS84). Solo el dominio de navegación exterior usa coordenadas (ADR-003).
public record Coordenada(BigDecimal latitud, BigDecimal longitud) {

    private static final BigDecimal LATITUD_MAXIMA = new BigDecimal("90");
    private static final BigDecimal LONGITUD_MAXIMA = new BigDecimal("180");

    public Coordenada {
        Objects.requireNonNull(latitud, "La latitud es obligatoria");
        Objects.requireNonNull(longitud, "La longitud es obligatoria");
        if (latitud.abs().compareTo(LATITUD_MAXIMA) > 0) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90");
        }
        if (longitud.abs().compareTo(LONGITUD_MAXIMA) > 0) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180");
        }
    }

    public static Coordenada de(double latitud, double longitud) {
        return new Coordenada(
                BigDecimal.valueOf(latitud).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(longitud).setScale(6, RoundingMode.HALF_UP));
    }

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    // Distancia en línea recta (haversine). Sirve para elegir las zonas más cercanas sin llamar al proveedor.
    public double distanciaMetrosA(Coordenada otra) {
        double lat1 = Math.toRadians(latitud.doubleValue());
        double lat2 = Math.toRadians(otra.latitud.doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(otra.longitud.doubleValue() - longitud.doubleValue());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.sqrt(h));
    }
}
