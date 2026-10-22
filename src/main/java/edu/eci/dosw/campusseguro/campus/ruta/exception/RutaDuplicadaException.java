package edu.eci.dosw.campusseguro.campus.ruta.exception;


public class RutaDuplicadaException extends RuntimeException {

    public RutaDuplicadaException(Long espacioId) {
        super("El espacio " + espacioId + " ya tiene una ruta asociada");
    }
}
