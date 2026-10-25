package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.PisoRequest;
import edu.eci.dosw.campusseguro.campus.dto.PisoResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RF-02: pisos de un edificio. El numero es unico por edificio y no se registran pisos en edificios inactivos.
@Slf4j
@Service
@RequiredArgsConstructor
public class PisoService {

    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;
    private final EspacioRepository espacioRepository;

    @Transactional
    public PisoResponse crear(UUID edificioId, PisoRequest request) {
        Edificio edificio = buscarEdificio(edificioId);

        if (edificio.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar pisos en un edificio inactivo");
        }

        if (pisoRepository.existsByEdificioIdAndNumero(edificioId, request.numero())) {
            throw new PisoDuplicadoException(request.numero());
        }

        Piso piso = new Piso();
        piso.setEdificioId(edificioId);
        piso.setNumero(request.numero());
        piso.setNombre(limpiar(request.nombre()));

        Piso guardado = pisoRepository.save(piso);
        log.info("Piso {} creado en el edificio {} con número {}",
                guardado.getId(), edificioId, guardado.getNumero());
        return aRespuesta(guardado);
    }

    @Transactional
    public PisoResponse modificar(UUID id, PisoRequest request) {
        Piso piso = buscar(id);

        if (pisoRepository.existsByEdificioIdAndNumeroAndIdNot(
                piso.getEdificioId(), request.numero(), id)) {
            throw new PisoDuplicadoException(request.numero());
        }

        piso.setNumero(request.numero());
        piso.setNombre(limpiar(request.nombre()));

        Piso guardado = pisoRepository.save(piso);
        log.info("Piso {} modificado", guardado.getId());
        return aRespuesta(guardado);
    }

    @Transactional
    public PisoResponse inactivar(UUID id) {
        Piso piso = buscar(id);

        if (espacioRepository.existsByPisoIdAndEstado(id, Estado.ACTIVO)) {
            log.warn("No se inactivó el piso {}: tiene espacios activos", id);
            throw new ReglaNegocioException("El piso tiene espacios activos; deben inactivarse antes");
        }

        piso.setEstado(Estado.INACTIVO);

        Piso guardado = pisoRepository.save(piso);
        log.info("Piso {} inactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional
    public PisoResponse reactivar(UUID id) {
        Piso piso = buscar(id);

        if (piso.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("El piso ya está activo");
        }

        if (buscarEdificio(piso.getEdificioId()).getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("El edificio está inactivo: debe reactivarse primero");
        }

        piso.setEstado(Estado.ACTIVO);

        Piso guardado = pisoRepository.save(piso);
        log.info("Piso {} reactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<PisoResponse> listarPorEdificio(UUID edificioId, boolean incluirInactivos) {
        buscarEdificio(edificioId);

        return pisoRepository.findByEdificioIdOrderByNumeroAsc(edificioId).stream()
                .filter(p -> incluirInactivos || p.getEstado() == Estado.ACTIVO)
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public PisoResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private Piso buscar(UUID id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new PisoNoEncontradoException(id));
    }

    private Edificio buscarEdificio(UUID id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private static String limpiar(String nombre) {
        return nombre == null || nombre.isBlank() ? null : nombre.trim();
    }

    private PisoResponse aRespuesta(Piso p) {
        return new PisoResponse(
                p.getId(),
                p.getEdificioId(),
                p.getNumero(),
                p.getNombre(),
                p.getEstado()
        );
    }
}