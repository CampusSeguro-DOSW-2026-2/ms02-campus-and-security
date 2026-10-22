package edu.eci.dosw.campusseguro.campus.ruta.integration;

import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

class EnMemoriaCampus {

    private final AtomicLong siguiente = new AtomicLong(100);

    private final Map<Long, EdificioInfo> edificios = new HashMap<>();
    private final Map<Long, EspacioInfo> espacios = new HashMap<>();
    private final Map<Long, ZonaInfo> zonas = new HashMap<>();

    void limpiar() {
        edificios.clear();
        espacios.clear();
        zonas.clear();
    }

    EdificioInfo edificio(String nombre, boolean activo) {
        EdificioInfo e = new EdificioInfo(siguiente.incrementAndGet(), nombre.substring(0, 1), nombre, activo);
        edificios.put(e.id(), e);
        return e;
    }

    EspacioInfo espacio(Long edificioId, String nombre, boolean activo) {
        EspacioInfo e = new EspacioInfo(siguiente.incrementAndGet(), edificioId, nombre, activo);
        espacios.put(e.id(), e);
        return e;
    }

    ZonaInfo zona(String nombre, boolean activa) {
        ZonaInfo z = new ZonaInfo(siguiente.incrementAndGet(), nombre, "Ubicación de " + nombre, activa);
        zonas.put(z.id(), z);
        return z;
    }

    void inactivarZona(Long id) {
        ZonaInfo z = zonas.get(id);
        zonas.put(id, new ZonaInfo(z.id(), z.nombre(), z.ubicacion(), false));
    }

    Optional<EdificioInfo> buscarEdificio(Long id) {
        return Optional.ofNullable(edificios.get(id));
    }

    Optional<EspacioInfo> buscarEspacio(Long id) {
        return Optional.ofNullable(espacios.get(id));
    }

    List<EspacioInfo> espaciosDe(Long edificioId) {
        return espacios.values().stream()
                .filter(e -> e.edificioId().equals(edificioId))
                .toList();
    }

    Optional<ZonaInfo> buscarZona(Long id) {
        return Optional.ofNullable(zonas.get(id));
    }
}
