package edu.eci.dosw.campusseguro.campus.exception;

public class PisoDuplicadoException extends RuntimeException {

    public PisoDuplicadoException(Integer numero) {
        super("Ya existe un piso con el número " + numero + " en este edificio");
    }
}
