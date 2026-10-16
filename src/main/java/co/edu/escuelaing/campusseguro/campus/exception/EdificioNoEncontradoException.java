package co.edu.escuelaing.campusseguro.campus.exception;

public class EdificioNoEncontradoException extends RuntimeException {

    public EdificioNoEncontradoException(Long id) {
        super("No existe un edificio con id " + id);
    }
}