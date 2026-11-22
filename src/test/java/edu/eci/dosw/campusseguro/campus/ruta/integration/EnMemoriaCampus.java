package edu.eci.dosw.campusseguro.campus.ruta.integration;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


class EnMemoriaCampus {

    

    private final Map<UUID, EdificioInfo> edificios = new HashMap<>();
    private final Map<UUID, EspacioInfo> espacios = new HashMap<>();
    private final Map<UUID, ZonaInfo> zonas = new HashMap<>();

    void limpiar() {
        edificios.clear();
        espacios.clear();
        zonas.clear();
    }

    EdificioInfo edificio(String nombre, boolean activo) {
        EdificioInfo e = new EdificioInfo(UUID.randomUUID(), nombre, activo);
        edificios.put(e.id(), e);
        return e;
    }

    // Con un id fijo, para que coincida con un edificio persistido de verdad (lo necesitan los
    // gateways de salida/escalera, que sí consultan la base real).
    EdificioInfo edificioConId(UUID id, String nombre, boolean activo) {
        EdificioInfo e = new EdificioInfo(id, nombre, activo);
        edificios.put(e.id(), e);
        return e;
    }

    EspacioInfo espacio(UUID edificioId, String nombre, boolean activo) {
        EspacioInfo e = new EspacioInfo(UUID.randomUUID(), edificioId, nombre, activo);
        espacios.put(e.id(), e);
        return e;
    }

    // Con piso, para que la guía automática no exija escalera (espacio y salida en el mismo piso).
    EspacioInfo espacioEnPiso(UUID edificioId, String nombre, boolean activo, UUID pisoId, int pisoNumero) {
        EspacioInfo e = new EspacioInfo(UUID.randomUUID(), edificioId, nombre, activo, null, pisoId, pisoNumero);
        espacios.put(e.id(), e);
        return e;
    }

    ZonaInfo zona(String nombre, boolean activa) {
        ZonaInfo z = new ZonaInfo(UUID.randomUUID(), nombre, "Ubicación de " + nombre, activa);
        zonas.put(z.id(), z);
        return z;
    }

    void inactivarZona(UUID id) {
        ZonaInfo z = zonas.get(id);
        zonas.put(id, new ZonaInfo(z.id(), z.nombre(), z.ubicacion(), false));
    }

    Optional<EdificioInfo> buscarEdificio(UUID id) {
        return Optional.ofNullable(edificios.get(id));
    }

    Optional<EspacioInfo> buscarEspacio(UUID id) {
        return Optional.ofNullable(espacios.get(id));
    }

    List<EspacioInfo> espaciosDe(UUID edificioId) {
        return espacios.values().stream()
                .filter(e -> e.edificioId().equals(edificioId))
                .toList();
    }

    Optional<ZonaInfo> buscarZona(UUID id) {
        return Optional.ofNullable(zonas.get(id));
    }
}
