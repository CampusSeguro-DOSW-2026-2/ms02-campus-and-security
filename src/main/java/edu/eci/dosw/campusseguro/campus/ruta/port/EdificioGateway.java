package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.UUID;

import java.util.Optional;

// Lo implementa la Persona 1 con el repositorio de edificios.
public interface EdificioGateway {

    Optional<EdificioInfo> buscarPorId(UUID id);

    record EdificioInfo(UUID id, String codigo, String nombre, boolean activo) {
    }
}
