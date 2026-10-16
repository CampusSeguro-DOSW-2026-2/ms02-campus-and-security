package co.edu.escuelaing.campusseguro.campus.exception;

public class ZonaSeguraNoEncontradaException extends RuntimeException {

    public ZonaSeguraNoEncontradaException(Long id) {
        super("No existe una zona segura con id " + id);
    }
}