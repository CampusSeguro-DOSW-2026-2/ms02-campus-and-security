package edu.eci.dosw.campusseguro.campus.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Campus;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaAbierta;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaAbiertaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.CampusRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaAbiertaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ADR-003: zonas abiertas de un campus. No se registran en campus inactivos.
// Las coordenadas son opcionales, pero latitud y longitud van juntas.
@Slf4j
@Service
@RequiredArgsConstructor
public class ZonaAbiertaService {

    private final ZonaAbiertaRepository zonaAbiertaRepository;
    private final CampusRepository campusRepository;
    private final ZonaSeguraRepository zonaSeguraRepository;

    @Transactional
    public ZonaAbiertaResponse crear(UUID campusId, ZonaAbiertaRequest request) {
        Campus campus = buscarCampus(campusId);

        if (campus.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar zonas abiertas en un campus inactivo");
        }

        validarCoordenadas(request.latitud(), request.longitud());

        ZonaAbierta zona = new ZonaAbierta();
        zona.setCampusId(campusId);
        zona.setNombre(request.nombre().trim());
        zona.setTipo(request.tipo());
        zona.setLatitud(request.latitud());
        zona.setLongitud(request.longitud());

        ZonaAbierta guardada = zonaAbiertaRepository.save(zona);
        log.info("Zona abierta {} creada en el campus {}", guardada.getId(), campusId);
        return aRespuesta(guardada);
    }

    @Transactional
    public ZonaAbiertaResponse modificar(UUID id, ZonaAbiertaRequest request) {
        ZonaAbierta zona = buscar(id);
        validarCoordenadas(request.latitud(), request.longitud());

        zona.setNombre(request.nombre().trim());
        zona.setTipo(request.tipo());
        zona.setLatitud(request.latitud());
        zona.setLongitud(request.longitud());

        ZonaAbierta guardada = zonaAbiertaRepository.save(zona);
        log.info("Zona abierta {} modificada", guardada.getId());
        return aRespuesta(guardada);
    }

    @Transactional
    public ZonaAbiertaResponse inactivar(UUID id) {
        ZonaAbierta zona = buscar(id);

        if (zonaSeguraRepository.existsByZonaAbiertaIdAndEstado(id, Estado.ACTIVO)) {
            log.warn("No se inactivó la zona abierta {}: tiene zonas seguras activas", id);
            throw new ReglaNegocioException("La zona abierta tiene zonas seguras activas; deben inactivarse antes");
        }

        zona.setEstado(Estado.INACTIVO);

        ZonaAbierta guardada = zonaAbiertaRepository.save(zona);
        log.info("Zona abierta {} inactivada", id);
        return aRespuesta(guardada);
    }

    @Transactional
    public ZonaAbiertaResponse reactivar(UUID id) {
        ZonaAbierta zona = buscar(id);

        if (zona.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("La zona abierta ya está activa");
        }
        if (buscarCampus(zona.getCampusId()).getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("El campus de la zona abierta está inactivo; reactívelo antes");
        }

        zona.setEstado(Estado.ACTIVO);

        ZonaAbierta guardada = zonaAbiertaRepository.save(zona);
        log.info("Zona abierta {} reactivada", id);
        return aRespuesta(guardada);
    }

    @Transactional(readOnly = true)
    public List<ZonaAbiertaResponse> listarPorCampus(UUID campusId, boolean incluirInactivos) {
        buscarCampus(campusId);
        return zonaAbiertaRepository.findByCampusIdOrderByNombreAsc(campusId).stream()
                .filter(z -> incluirInactivos || z.getEstado() == Estado.ACTIVO)
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ZonaAbiertaResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private void validarCoordenadas(BigDecimal latitud, BigDecimal longitud) {
        if ((latitud == null) != (longitud == null)) {
            throw new ReglaNegocioException("La latitud y la longitud deben enviarse juntas");
        }
    }

    private Campus buscarCampus(UUID id) {
        return campusRepository.findById(id)
                .orElseThrow(() -> new CampusNoEncontradoException(id));
    }

    private ZonaAbierta buscar(UUID id) {
        return zonaAbiertaRepository.findById(id)
                .orElseThrow(() -> new ZonaAbiertaNoEncontradaException(id));
    }

    private ZonaAbiertaResponse aRespuesta(ZonaAbierta z) {
        return new ZonaAbiertaResponse(z.getId(), z.getCampusId(), z.getNombre(),
                z.getTipo(), z.getLatitud(), z.getLongitud(), z.getEstado());
    }
}
