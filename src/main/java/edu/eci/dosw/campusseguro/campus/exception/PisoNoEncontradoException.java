package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class PisoNoEncontradoException extends RuntimeException {

    public PisoNoEncontradoException(UUID id) {
        super("No existe un piso con id " + id);
    }
}
