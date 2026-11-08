package edu.eci.dosw.campusseguro.campus.ruta.port;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;

// Puerto de navegación exterior (ADR-003, D4). El dominio y los casos de uso dependen de esta interfaz,
// nunca de OpenStreetMap ni de otro proveedor concreto.
public interface ProveedorNavegacion {

    /**
     * Calcula el recorrido peatonal entre dos puntos exteriores.
     *
     * @throws edu.eci.dosw.campusseguro.campus.ruta.exception.RutaNoEncontradaException      si no existe recorrido
     * @throws edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException si el proveedor falla
     */
    RutaCalculada calcularRutaPeatonal(Coordenada origen, Coordenada destino);
}
