package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.CrearRutaRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaDuplicadaException;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.dto.ActualizarRutaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RutaService {

    private static final Logger log = LoggerFactory.getLogger(RutaService.class);

    private final RutaRepository rutaRepository;
    private final EspacioGateway espacioGateway;
    private final ZonaSeguraGateway zonaGateway;

    public RutaService(RutaRepository rutaRepository,
                       EspacioGateway espacioGateway,
                       ZonaSeguraGateway zonaGateway) {
        this.rutaRepository = rutaRepository;
        this.espacioGateway = espacioGateway;
        this.zonaGateway = zonaGateway;
    }

    @Transactional
    public RutaResponse crear(CrearRutaRequest request) {
        EspacioInfo espacio = espacioGateway.buscarPorId(request.espacioOrigenId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El espacio " + request.espacioOrigenId() + " no existe"));
        if (!espacio.disponible()) {
            throw new ReglaNegocioException("El espacio '" + espacio.nombre() + "' no está disponible");
        }

        ZonaInfo zona = zonaGateway.buscarPorId(request.zonaSeguraDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La zona segura " + request.zonaSeguraDestinoId() + " no existe"));
        if (!zona.activa()) {
            throw new ReglaNegocioException("La zona segura '" + zona.nombre() + "' está inactiva");
        }

        if (request.rutaAlternativaId() != null && !rutaRepository.existsById(request.rutaAlternativaId())) {
            throw new RecursoNoEncontradoException(
                    "La ruta alternativa " + request.rutaAlternativaId() + " no existe");
        }

        if (rutaRepository.existsByEspacioOrigenId(request.espacioOrigenId())) {
            throw new RutaDuplicadaException(request.espacioOrigenId());
        }

        String motivo = motivoPara(request.estado(), request.motivo());

        RutaEvacuacion ruta = new RutaEvacuacion(request.espacioOrigenId(), request.zonaSeguraDestinoId(),
                request.rutaAlternativaId(), request.descripcion().trim(), request.estado());
        ruta.cambiarEstado(request.estado(), motivo);
        try {
            ruta = rutaRepository.saveAndFlush(ruta);
        } catch (DataIntegrityViolationException e) {
            // otra solicitud creo la ruta al mismo tiempo
            throw new RutaDuplicadaException(request.espacioOrigenId());
        }
        log.info("Ruta {} creada para el espacio {} con estado {}",
                ruta.getId(), ruta.getEspacioOrigenId(), ruta.getEstado());
        return RutaResponse.desde(ruta);
    }

    @Transactional
    public RutaResponse actualizar(UUID id, ActualizarRutaRequest request) {
        RutaEvacuacion ruta = buscar(id);

        ZonaInfo zona = zonaGateway.buscarPorId(request.zonaSeguraDestinoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                       "La zona segura " + request.zonaSeguraDestinoId() + " no existe"));

        if (!zona.activa()) {
            throw new ReglaNegocioException(
                "La zona segura '" + zona.nombre() + "' está inactiva");
        }

        ruta.setZonaSeguraDestinoId(request.zonaSeguraDestinoId());
        ruta.setDescripcion(request.descripcion().trim());

        RutaEvacuacion actualizada = rutaRepository.save(ruta);

        log.info("Ruta {} actualizada con destino {}", id, zona.id());

        return RutaResponse.desde(actualizada);
    }

    @Transactional
    public RutaResponse cambiarEstado(UUID id, EstadoRuta nuevoEstado, String motivo) {
        RutaEvacuacion ruta = buscar(id);
        String motivoValido = motivoPara(nuevoEstado, motivo);
        if (nuevoEstado != EstadoRuta.BLOQUEADA) {
            validarOrigenYDestinoUtilizables(ruta);
        }
        EstadoRuta anterior = ruta.getEstado();
        ruta.cambiarEstado(nuevoEstado, motivoValido);
        RutaEvacuacion guardada = rutaRepository.save(ruta);
        log.info("Ruta {} cambió de estado {} a {}", id, anterior, nuevoEstado);
        return RutaResponse.desde(guardada);
    }

    @Transactional(readOnly = true)
    public RutaResponse obtener(UUID id) {
        return RutaResponse.desde(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<RutaResponse> listar() {
        return rutaRepository.findAll().stream().map(RutaResponse::desde).toList();
    }

    private void validarOrigenYDestinoUtilizables(RutaEvacuacion ruta) {
        boolean espacioDisponible = espacioGateway.buscarPorId(ruta.getEspacioOrigenId())
                .map(EspacioInfo::disponible)
                .orElse(false);
        if (!espacioDisponible) {
            throw new ReglaNegocioException("El espacio de origen no está disponible; la ruta solo puede quedar bloqueada");
        }
        boolean zonaActiva = zonaGateway.buscarPorId(ruta.getZonaSeguraDestinoId())
                .map(ZonaInfo::activa)
                .orElse(false);
        if (!zonaActiva) {
            throw new ReglaNegocioException("La zona segura de destino está inactiva; la ruta solo puede quedar bloqueada");
        }
    }

    private static String motivoPara(EstadoRuta estado, String motivo) {
        if (estado == EstadoRuta.DISPONIBLE) {
            return null;
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("El motivo es obligatorio cuando la ruta queda bloqueada o restringida");
        }
        return motivo.trim();
    }

    private RutaEvacuacion buscar(UUID id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La ruta " + id + " no existe"));
    }
}
