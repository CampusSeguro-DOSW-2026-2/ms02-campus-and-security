package edu.eci.dosw.campusseguro.campus.exception;

public class SalidaDuplicadaException extends RuntimeException {
    public SalidaDuplicadaException(String codigo) {
        super("Ya existe una salida con el código " + codigo + " en este edificio");
    }
}
