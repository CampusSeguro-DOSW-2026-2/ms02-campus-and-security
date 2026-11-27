package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class SalidaNoEncontradaException extends RuntimeException {
    public SalidaNoEncontradaException(UUID id) {
        super("No existe una salida con id " + id);
    }
}
