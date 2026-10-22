package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.List;
import java.util.Optional;

// Lo implementa la Persona 2 con el repositorio de espacios (no con su servicio).
public interface EspacioGateway {

    Optional<EspacioInfo> buscarPorId(Long id);

    List<EspacioInfo> listarPorEdificio(Long edificioId);

    record EspacioInfo(Long id, Long edificioId, String nombre, boolean activo) {
    }
}
