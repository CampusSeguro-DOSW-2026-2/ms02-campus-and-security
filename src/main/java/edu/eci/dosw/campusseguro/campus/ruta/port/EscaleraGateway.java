package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.List;
import java.util.UUID;

// Puerto para que el módulo de rutas consulte escaleras sin depender de sus repositorios.
public interface EscaleraGateway {

    List<EscaleraInfo> listarDisponiblesQueConectan(
            UUID edificioId, UUID pisoOrigenId, UUID pisoDestinoId);

    record EscaleraInfo(
            UUID id,
            String codigo,
            String nombre,
            UUID edificioId,
            List<UUID> pisoIds
    ) {
    }
}
