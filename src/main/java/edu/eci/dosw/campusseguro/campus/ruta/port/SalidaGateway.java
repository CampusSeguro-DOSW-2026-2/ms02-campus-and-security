package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalidaGateway {

    Optional<SalidaInfo> buscarPorId(UUID id);

    List<SalidaInfo> listarPorEdificio(UUID edificioId);

    record SalidaInfo(UUID id, UUID edificioId, UUID pisoId, String nombre,
            BigDecimal latitud, BigDecimal longitud, boolean disponible) {
    }
}
