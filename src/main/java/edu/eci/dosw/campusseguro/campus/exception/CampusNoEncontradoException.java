package edu.eci.dosw.campusseguro.campus.exception;

import java.util.UUID;

public class CampusNoEncontradoException extends RuntimeException {

    public CampusNoEncontradoException(UUID id) {
        super("No existe un campus con id " + id);
    }
}
