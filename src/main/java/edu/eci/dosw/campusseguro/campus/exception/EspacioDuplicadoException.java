package edu.eci.dosw.campusseguro.campus.exception;

public class EspacioDuplicadoException extends RuntimeException {

    public EspacioDuplicadoException(String codigo) {
        super("Ya existe un espacio con el código " + codigo + " en este edificio");
    }
}
