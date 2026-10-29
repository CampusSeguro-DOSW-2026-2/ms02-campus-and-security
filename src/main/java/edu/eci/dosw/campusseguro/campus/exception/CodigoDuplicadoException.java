package edu.eci.dosw.campusseguro.campus.exception;

public class CodigoDuplicadoException extends RuntimeException {

    public CodigoDuplicadoException(String codigo) {
        super("Ya existe un edificio con el código " + codigo);
    }
}