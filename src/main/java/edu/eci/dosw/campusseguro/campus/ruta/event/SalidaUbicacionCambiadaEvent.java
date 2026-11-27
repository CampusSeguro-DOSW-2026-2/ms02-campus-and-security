package edu.eci.dosw.campusseguro.campus.ruta.event;

import java.util.UUID;

// Se publica cuando una salida se crea, se mueve o se reactiva: sus rutas exteriores deben generarse o recalcularse.
public record SalidaUbicacionCambiadaEvent(UUID salidaId) {
}
