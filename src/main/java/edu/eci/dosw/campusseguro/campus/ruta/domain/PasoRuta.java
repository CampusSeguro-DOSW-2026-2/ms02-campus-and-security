package edu.eci.dosw.campusseguro.campus.ruta.domain;

// Una indicación del recorrido exterior ("Gira a la izquierda por Calle 1").
public record PasoRuta(String instruccion, double distanciaMetros, double duracionSegundos) {
}
