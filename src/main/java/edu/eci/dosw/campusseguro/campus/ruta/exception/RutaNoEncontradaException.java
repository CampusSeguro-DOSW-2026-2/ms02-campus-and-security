package edu.eci.dosw.campusseguro.campus.ruta.exception;

// El proveedor respondió bien, pero no existe un recorrido peatonal entre los dos puntos.
public class RutaNoEncontradaException extends RuntimeException {

    public RutaNoEncontradaException() {
        super("No existe un recorrido peatonal entre la salida y la zona segura");
    }
}
