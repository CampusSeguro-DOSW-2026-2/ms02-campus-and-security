package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.Optional;

// Lo implementa la Persona 1 con el repositorio de zonas seguras.
public interface ZonaSeguraGateway {

    Optional<ZonaInfo> buscarPorId(Long id);

    record ZonaInfo(Long id, String nombre, String ubicacion, boolean activa) {
    }
}
