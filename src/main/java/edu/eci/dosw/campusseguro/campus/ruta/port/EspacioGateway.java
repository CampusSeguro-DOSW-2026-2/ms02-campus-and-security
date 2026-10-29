package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.UUID;

import java.util.List;
import java.util.Optional;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;

// Lo implementa la Persona 2 con el repositorio de espacios (no con su servicio).
public interface EspacioGateway {

    Optional<EspacioInfo> buscarPorId(UUID id);

    List<EspacioInfo> listarPorEdificio(UUID edificioId);

    record EspacioInfo(UUID id, UUID edificioId, String nombre,
            boolean disponible, TipoEspacio tipoEspacio) {
        public EspacioInfo(UUID id, UUID edificioId, String nombre, boolean disponible) {
            this(id, edificioId, nombre, disponible, null);
        }
    }
}
