package edu.eci.dosw.campusseguro.campus.exception;

public class ZonaSeguraNoEncontradaException extends RuntimeException {

    public ZonaSeguraNoEncontradaException(Long id) {
        super("No existe una zona segura con id " + id);
    }
}