package edu.eci.dosw.campusseguro.campus.ruta.exception;

import java.util.UUID;


public class RutaDuplicadaException extends RuntimeException {

    public RutaDuplicadaException(UUID espacioId) {
        super("El espacio " + espacioId + " ya tiene una ruta asociada");
    }
}
