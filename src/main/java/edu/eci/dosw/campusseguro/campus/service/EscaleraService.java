package edu.eci.dosw.campusseguro.campus.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.dto.CambiarDisponibilidadEscaleraRequest;
import edu.eci.dosw.campusseguro.campus.dto.CambiarPrioridadEscaleraRequest;
import edu.eci.dosw.campusseguro.campus.dto.EscaleraRequest;
import edu.eci.dosw.campusseguro.campus.dto.EscaleraResponse;
import edu.eci.dosw.campusseguro.campus.entity.DisponibilidadEscalera;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Escalera;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EscaleraRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscaleraService {

    private final EscaleraRepository escaleraRepository;
    private final EdificioRepository edificioRepository;
    private final PisoRepository pisoRepository;

    @Transactional
    public EscaleraResponse crear(UUID edificioId, EscaleraRequest request) {
        Edificio edificio = buscarEdificio(edificioId);

        if (edificio.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException(
                    "No se pueden registrar escaleras en un edificio inactivo");
        }

        List<Piso> pisos = validarPisos(edificioId, request.pisoIds());

        Escalera escalera = new Escalera();
        escalera.setEdificioId(edificioId);
        escalera.setCodigo(generarCodigo());
        escalera.setNombre(request.nombre().trim());
        escalera.setPisos(new HashSet<>(pisos));
        escalera.setPrioridad(request.prioridad() != null
                ? request.prioridad() : siguientePrioridad(edificioId));

        Escalera guardada = escaleraRepository.save(escalera);
        log.info("Escalera {} creada en el edificio {}", guardada.getCodigo(), edificioId);
        return aRespuesta(guardada);
    }

    @Transactional(readOnly = true)
    public List<EscaleraResponse> listarPorEdificio(UUID edificioId, boolean incluirInactivas) {
        buscarEdificio(edificioId);

        return escaleraRepository.findByEdificioId(edificioId).stream()
                .filter(e -> incluirInactivas || e.getEstado() == Estado.ACTIVO)
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EscaleraResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    @Transactional
    public EscaleraResponse cambiarDisponibilidad(
            UUID id, CambiarDisponibilidadEscaleraRequest request) {
        Escalera escalera = buscar(id);

        String motivo = request.motivo() == null ? null : request.motivo().trim();

        if (request.disponibilidad() != DisponibilidadEscalera.DISPONIBLE
                && (motivo == null || motivo.isBlank())) {
            throw new ReglaNegocioException(
                    "Debe indicar el motivo para bloquear o poner fuera de servicio la escalera");
        }

        escalera.setDisponibilidad(request.disponibilidad());
        escalera.setMotivo(request.disponibilidad() == DisponibilidadEscalera.DISPONIBLE
                ? null : motivo);

        Escalera guardada = escaleraRepository.save(escalera);
        log.info("Disponibilidad de escalera {} actualizada a {}",
                guardada.getCodigo(), guardada.getDisponibilidad());
        return aRespuesta(guardada);
    }

    @Transactional
    public EscaleraResponse cambiarPrioridad(UUID id, CambiarPrioridadEscaleraRequest request) {
        Escalera escalera = buscar(id);
        escalera.setPrioridad(request.prioridad());
        Escalera guardada = escaleraRepository.save(escalera);
        log.info("Prioridad de escalera {} actualizada a {}", guardada.getCodigo(), guardada.getPrioridad());
        return aRespuesta(guardada);
    }

    // Sin prioridad explicita, la escalera nueva queda despues de las que ya tiene el edificio.
    private int siguientePrioridad(UUID edificioId) {
        return escaleraRepository.findByEdificioId(edificioId).stream()
                .filter(e -> e.getEstado() == Estado.ACTIVO)
                .mapToInt(Escalera::getPrioridad)
                .max().orElse(0) + 1;
    }

    @Transactional
    public EscaleraResponse inactivar(UUID id) {
        Escalera escalera = buscar(id);

        if (escalera.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("La escalera ya está inactiva");
        }

        escalera.setEstado(Estado.INACTIVO);
        return aRespuesta(escaleraRepository.save(escalera));
    }

    @Transactional
    public EscaleraResponse reactivar(UUID id) {
        Escalera escalera = buscar(id);

        if (escalera.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("La escalera ya está activa");
        }

        Edificio edificio = buscarEdificio(escalera.getEdificioId());
        if (edificio.getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException(
                    "El edificio está inactivo: debe reactivarse primero");
        }

        if (escalera.getPisos().stream().anyMatch(p -> p.getEstado() != Estado.ACTIVO)) {
            throw new ReglaNegocioException(
                    "Uno o más pisos conectados están inactivos: deben reactivarse primero");
        }

        escalera.setEstado(Estado.ACTIVO);
        return aRespuesta(escaleraRepository.save(escalera));
    }

    private List<Piso> validarPisos(UUID edificioId, List<UUID> pisoIds) {
        if (pisoIds == null || pisoIds.size() < 2) {
            throw new ReglaNegocioException(
                    "Una escalera debe conectar al menos dos pisos");
        }

        Set<UUID> unicos = new HashSet<>(pisoIds);
        if (unicos.size() != pisoIds.size()) {
            throw new ReglaNegocioException(
                    "La lista de pisos contiene identificadores duplicados");
        }

        List<Piso> pisos = pisoIds.stream()
                .map(id -> pisoRepository.findById(id)
                        .orElseThrow(() -> new PisoNoEncontradoException(id)))
                .toList();

        for (Piso piso : pisos) {
            if (!piso.getEdificioId().equals(edificioId)) {
                throw new ReglaNegocioException(
                        "Todos los pisos conectados deben pertenecer al mismo edificio");
            }
            if (piso.getEstado() != Estado.ACTIVO) {
                throw new ReglaNegocioException(
                        "No se pueden conectar pisos inactivos");
            }
        }

        return pisos;
    }

    private String generarCodigo() {
        String codigo;
        do {
            codigo = "ESC-" + UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (escaleraRepository.existsByCodigo(codigo));
        return codigo;
    }

    private Escalera buscar(UUID id) {
        return escaleraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La escalera " + id + " no existe"));
    }

    private Edificio buscarEdificio(UUID id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private EscaleraResponse aRespuesta(Escalera escalera) {
        List<UUID> pisoIds = escalera.getPisos().stream()
                .map(Piso::getId)
                .sorted()
                .collect(Collectors.toList());

        return new EscaleraResponse(
                escalera.getId(),
                escalera.getEdificioId(),
                escalera.getCodigo(),
                escalera.getNombre(),
                escalera.getDisponibilidad(),
                escalera.getMotivo(),
                escalera.getEstado(),
                escalera.getPrioridad(),
                pisoIds);
    }
}
