package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class ZonaAbiertaNoEncontradaException extends RuntimeException {

    public ZonaAbiertaNoEncontradaException(UUID id) {
        super("No existe una zona abierta con id " + id);
    }
}
