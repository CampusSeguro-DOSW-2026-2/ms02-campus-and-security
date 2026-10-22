package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.UUID;


// RF-06: lo usan las Personas 1 y 2 antes de eliminar un edificio, espacio o zona.
public interface VerificadorRutas {

    boolean tieneRutaEspacio(UUID espacioId);

    boolean tieneRutaZonaSegura(UUID zonaSeguraId);

    boolean tieneRutaEdificio(UUID edificioId);
}
