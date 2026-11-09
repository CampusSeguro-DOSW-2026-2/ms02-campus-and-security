package edu.eci.dosw.campusseguro.campus.ruta.event;

import java.util.UUID;

// Se publica cuando una zona segura con coordenadas se crea, se mueve o se reactiva.
public record ZonaSeguraUbicacionCambiadaEvent(UUID zonaId) {
}
