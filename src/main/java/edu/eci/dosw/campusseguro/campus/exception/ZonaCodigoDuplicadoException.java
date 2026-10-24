package edu.eci.dosw.campusseguro.campus.exception;

public class ZonaCodigoDuplicadoException extends RuntimeException {

    public ZonaCodigoDuplicadoException(String codigo) {
        super("Ya existe una zona segura con el código " + codigo);
    }
}