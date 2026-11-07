package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.CampusRequest;
import edu.eci.dosw.campusseguro.campus.dto.CampusResponse;
import edu.eci.dosw.campusseguro.campus.entity.Campus;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.CampusRepository;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaAbiertaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ADR-003: campus. No se inactiva un campus con edificios o zonas abiertas activas.
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusService {

    private final CampusRepository campusRepository;
    private final EdificioRepository edificioRepository;
    private final ZonaAbiertaRepository zonaAbiertaRepository;

    @Transactional
    public CampusResponse crear(CampusRequest request) {
        Campus campus = new Campus();
        campus.setNombre(request.nombre().trim());
        campus.setDescripcion(request.descripcion());

        Campus guardado = campusRepository.save(campus);
        log.info("Campus {} creado", guardado.getId());
        return aRespuesta(guardado);
    }

    @Transactional
    public CampusResponse modificar(UUID id, CampusRequest request) {
        Campus campus = buscar(id);
        campus.setNombre(request.nombre().trim());
        campus.setDescripcion(request.descripcion());

        Campus guardado = campusRepository.save(campus);
        log.info("Campus {} modificado", guardado.getId());
        return aRespuesta(guardado);
    }

    @Transactional
    public CampusResponse inactivar(UUID id) {
        Campus campus = buscar(id);

        if (edificioRepository.existsByCampusIdAndEstado(id, Estado.ACTIVO)) {
            log.warn("No se inactivó el campus {}: tiene edificios activos", id);
            throw new ReglaNegocioException("El campus tiene edificios activos; deben inactivarse antes");
        }
        if (zonaAbiertaRepository.existsByCampusIdAndEstado(id, Estado.ACTIVO)) {
            log.warn("No se inactivó el campus {}: tiene zonas abiertas activas", id);
            throw new ReglaNegocioException("El campus tiene zonas abiertas activas; deben inactivarse antes");
        }

        campus.setEstado(Estado.INACTIVO);

        Campus guardado = campusRepository.save(campus);
        log.info("Campus {} inactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional
    public CampusResponse reactivar(UUID id) {
        Campus campus = buscar(id);

        if (campus.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("El campus ya está activo");
        }

        campus.setEstado(Estado.ACTIVO);

        Campus guardado = campusRepository.save(campus);
        log.info("Campus {} reactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> listar(boolean incluirInactivos) {
        return campusRepository.findAll().stream()
                .filter(c -> incluirInactivos || c.getEstado() == Estado.ACTIVO)
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampusResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private Campus buscar(UUID id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new CampusNoEncontradoException(id));
    }

    private CampusResponse aRespuesta(Campus c) {
        return new CampusResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getEstado());
    }
}
