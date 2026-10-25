package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.ZonaSegura;
import edu.eci.dosw.campusseguro.campus.exception.ZonaCodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZonaSeguraService {

    private final ZonaSeguraRepository zonaSeguraRepository;
    private final VerificadorRutas verificadorRutas;

    @Transactional
    public ZonaSeguraResponse crear(ZonaSeguraRequest request) {
        String codigo = request.codigo().trim();

        if (zonaSeguraRepository.existsByCodigo(codigo)) {
            throw new ZonaCodigoDuplicadoException(codigo);
        }

        ZonaSegura zona = new ZonaSegura();
        copiarDatos(zona, request);

        ZonaSegura guardada = zonaSeguraRepository.save(zona);
        log.info("Zona segura {} creada con código {}", guardada.getId(), guardada.getCodigo());
        return aRespuesta(guardada);
    }

    @Transactional
    public ZonaSeguraResponse modificar(UUID id, ZonaSeguraRequest request) {
        ZonaSegura zona = buscar(id);
        String codigo = request.codigo().trim();

        if (zonaSeguraRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ZonaCodigoDuplicadoException(codigo);
        }

        copiarDatos(zona, request);

        ZonaSegura guardada = zonaSeguraRepository.save(zona);
        log.info("Zona segura {} modificada", guardada.getId());
        return aRespuesta(guardada);
    }

    @Transactional
    public ZonaSeguraResponse inactivar(UUID id) {
        ZonaSegura zona = buscar(id);

        List<UUID> rutasActivas =
                verificadorRutas.rutasActivasDeZonaSegura(id);

        if (!rutasActivas.isEmpty()) {
            log.warn("No se inactivó la zona segura {}: es destino de rutas activas {}", id, rutasActivas);
            throw new ReglaNegocioException(
                    "La zona segura es destino de rutas activas " + rutasActivas
                            + ": deben bloquearse antes de inactivarla");
        }

        zona.setEstado(Estado.INACTIVO);

        ZonaSegura guardada = zonaSeguraRepository.save(zona);
        log.info("Zona segura {} inactivada", id);
        return aRespuesta(guardada);
    }

    @Transactional
    public ZonaSeguraResponse reactivar(UUID id) {
        ZonaSegura zona = buscar(id);

        if (zona.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException(
                    "La zona segura ya está activa");
        }

        zona.setEstado(Estado.ACTIVO);

        ZonaSegura guardada = zonaSeguraRepository.save(zona);
        log.info("Zona segura {} reactivada", id);
        return aRespuesta(guardada);
    }

    @Transactional(readOnly = true)
    public List<ZonaSeguraResponse> listar(boolean incluirInactivos) {
        return zonaSeguraRepository.findAll().stream()
                .filter(z -> incluirInactivos || z.getEstado() == Estado.ACTIVO)
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ZonaSeguraResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private ZonaSegura buscar(UUID id) {
        return zonaSeguraRepository.findById(id)
                .orElseThrow(() -> new ZonaSeguraNoEncontradaException(id));
    }

    private void copiarDatos(ZonaSegura zona, ZonaSeguraRequest request) {
        zona.setCodigo(request.codigo().trim());
        zona.setNombre(request.nombre().trim());
        zona.setUbicacion(request.ubicacion().trim());
        zona.setDescripcion(request.descripcion().trim());
        zona.setCapacidad(request.capacidad());
    }

    private ZonaSeguraResponse aRespuesta(ZonaSegura z) {
        return new ZonaSeguraResponse(
                z.getId(),
                z.getCodigo(),
                z.getNombre(),
                z.getUbicacion(),
                z.getDescripcion(),
                z.getCapacidad(),
                z.getEstado());
    }
}