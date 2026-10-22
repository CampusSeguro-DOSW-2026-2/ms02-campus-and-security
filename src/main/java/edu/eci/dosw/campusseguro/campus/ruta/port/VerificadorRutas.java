package edu.eci.dosw.campusseguro.campus.ruta.port;


// RF-06: lo usan las Personas 1 y 2 antes de eliminar un edificio, espacio o zona.
public interface VerificadorRutas {

    boolean tieneRutaEspacio(Long espacioId);

    boolean tieneRutaZonaSegura(Long zonaSeguraId);

    boolean tieneRutaEdificio(Long edificioId);
}
