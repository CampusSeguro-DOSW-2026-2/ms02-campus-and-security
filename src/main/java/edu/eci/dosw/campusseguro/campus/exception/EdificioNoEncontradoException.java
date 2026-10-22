package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class EdificioNoEncontradoException extends RuntimeException {

    public EdificioNoEncontradoException(UUID id) {
        super("No existe un edificio con id " + id);
    }
}