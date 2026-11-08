package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import java.util.List;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.PasoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;
import edu.eci.dosw.campusseguro.campus.ruta.port.ProveedorNavegacion;

// Proveedor falso para pruebas (ADR-003, D4): calcula una línea recta a pie, sin red. Los casos de uso lo usan en vez de OSM.
public class FakeProveedorNavegacion implements ProveedorNavegacion {

    private static final double RADIO_TIERRA_METROS = 6_371_000;
    private static final double VELOCIDAD_PEATON_MS = 1.4;

    private final RuntimeException fallo;
    private int llamadas;

    public FakeProveedorNavegacion() {
        this(null);
    }

    private FakeProveedorNavegacion(RuntimeException fallo) {
        this.fallo = fallo;
    }

    public static FakeProveedorNavegacion fallandoCon(RuntimeException fallo) {
        return new FakeProveedorNavegacion(fallo);
    }

    public int llamadas() {
        return llamadas;
    }

    @Override
    public RutaCalculada calcularRutaPeatonal(Coordenada origen, Coordenada destino) {
        llamadas++;
        if (fallo != null) {
            throw fallo;
        }
        double distancia = distanciaMetros(origen, destino);
        double duracion = distancia / VELOCIDAD_PEATON_MS;
        return new RutaCalculada(distancia, duracion, List.of(origen, destino), List.of(
                new PasoRuta("Inicia el recorrido", distancia, duracion),
                new PasoRuta("Has llegado al destino", 0, 0)));
    }

    // Fórmula de haversine.
    private double distanciaMetros(Coordenada a, Coordenada b) {
        double lat1 = Math.toRadians(a.latitud().doubleValue());
        double lat2 = Math.toRadians(b.latitud().doubleValue());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.longitud().doubleValue() - a.longitud().doubleValue());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_METROS * Math.asin(Math.sqrt(h));
    }
}
