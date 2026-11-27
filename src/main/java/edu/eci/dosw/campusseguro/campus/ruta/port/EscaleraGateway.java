package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.List;
import java.util.UUID;

// Puerto para que el módulo de rutas consulte escaleras sin depender de sus repositorios.
public interface EscaleraGateway {

    List<EscaleraInfo> listarDisponiblesQueConectan(
            UUID edificioId, UUID pisoOrigenId, UUID pisoDestinoId);

    // Recorrido de escaleras entre dos pisos: una lista por tramo (de un piso al siguiente), con las
    // escaleras disponibles de cada tramo ordenadas por prioridad (la primera es la recomendada).
    // Lista vacia si no se puede llegar. Los dos pisos deben ser distintos.
    List<List<EscaleraInfo>> tramosEntre(UUID edificioId, UUID pisoOrigenId, UUID pisoDestinoId);

    record EscaleraInfo(
            UUID id,
            String codigo,
            String nombre,
            UUID edificioId,
            List<UUID> pisoIds,
            int prioridad
    ) {
        public EscaleraInfo(UUID id, String codigo, String nombre, UUID edificioId, List<UUID> pisoIds) {
            this(id, codigo, nombre, edificioId, pisoIds, 1);
        }
    }
}
