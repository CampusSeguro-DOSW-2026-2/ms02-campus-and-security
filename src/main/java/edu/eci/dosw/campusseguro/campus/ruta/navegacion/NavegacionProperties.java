package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Configuración del proveedor de navegación (prefijo campusseguro.navigation). Cambiar de servidor es solo configuración.
@ConfigurationProperties(prefix = "campusseguro.navigation")
public record NavegacionProperties(
        @DefaultValue("https://routing.openstreetmap.de/routed-foot") String baseUrl,
        @DefaultValue("foot") String profile,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("10s") Duration readTimeout
) {
}
