package edu.eci.dosw.campusseguro.campus.ruta.exception;

// El proveedor de navegación no respondió o devolvió un error (timeout, caído, respuesta inválida).
public class NavegacionNoDisponibleException extends RuntimeException {

    public NavegacionNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public NavegacionNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
