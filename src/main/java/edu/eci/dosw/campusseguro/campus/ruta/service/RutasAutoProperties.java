package edu.eci.dosw.campusseguro.campus.ruta.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Generación automática de rutas exteriores (prefijo campusseguro.rutas.auto).
// enabled: si es false, no se generan rutas al crear o mover salidas y zonas (el endpoint manual sigue disponible).
// maxZonasPorSalida: cada salida se conecta con sus N zonas seguras más cercanas en línea recta.
@ConfigurationProperties(prefix = "campusseguro.rutas.auto")
public record RutasAutoProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("3") int maxZonasPorSalida
) {
}
