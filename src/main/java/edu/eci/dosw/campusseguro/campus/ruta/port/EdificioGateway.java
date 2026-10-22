package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.Optional;

// Lo implementa la Persona 1 con el repositorio de edificios.
public interface EdificioGateway {

    Optional<EdificioInfo> buscarPorId(Long id);

    record EdificioInfo(Long id, String codigo, String nombre, boolean activo) {
    }
}
