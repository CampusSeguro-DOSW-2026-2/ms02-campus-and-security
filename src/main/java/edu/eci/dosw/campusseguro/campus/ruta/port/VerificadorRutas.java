package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.List;
import java.util.UUID;

public interface VerificadorRutas {

    List<UUID> rutasActivasDeEspacio(UUID espacioId);

    List<UUID> rutasActivasDeZonaSegura(UUID zonaSeguraId);

    List<UUID> rutasActivasDeEdificio(UUID edificioId);
}
