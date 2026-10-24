package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class EspacioNoEncontradoException extends RuntimeException {

    public EspacioNoEncontradoException(UUID id) {
        super("No existe un espacio con id " + id);
    }
}
