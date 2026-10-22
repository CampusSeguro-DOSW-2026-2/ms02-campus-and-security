package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class ZonaSeguraNoEncontradaException extends RuntimeException {

    public ZonaSeguraNoEncontradaException(UUID id) {
        super("No existe una zona segura con id " + id);
    }
}