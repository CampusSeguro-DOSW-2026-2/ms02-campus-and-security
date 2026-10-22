package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.UUID;

import java.util.Optional;

// Lo implementa la Persona 1 con el repositorio de zonas seguras.
public interface ZonaSeguraGateway {

    Optional<ZonaInfo> buscarPorId(UUID id);

    record ZonaInfo(UUID id, String nombre, String ubicacion, boolean activa) {
    }
}
