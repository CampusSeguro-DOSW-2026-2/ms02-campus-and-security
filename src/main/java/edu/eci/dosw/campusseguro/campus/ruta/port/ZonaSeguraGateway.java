package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.math.BigDecimal;
import java.util.UUID;

import java.util.Optional;

// Lo implementa la Persona 1 con el repositorio de zonas seguras.
public interface ZonaSeguraGateway {

    Optional<ZonaInfo> buscarPorId(UUID id);

    // Las coordenadas pueden ser null: sin ellas la zona no puede ser destino de una ruta exterior.
    record ZonaInfo(UUID id, String nombre, String ubicacion, boolean activa,
            BigDecimal latitud, BigDecimal longitud) {

        public ZonaInfo(UUID id, String nombre, String ubicacion, boolean activa) {
            this(id, nombre, ubicacion, activa, null, null);
        }

        public boolean tieneCoordenadas() {
            return latitud != null && longitud != null;
        }
    }
}
