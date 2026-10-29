package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.eci.dosw.campusseguro.campus.dto.EspacioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEspacioRequest;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspacioService {

    private final EspacioRepository espacioRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;
    private final VerificadorRutas verificadorRutas;

    @Transactional
    public EspacioResponse crear(UUID pisoId, EspacioRequest request) {
        Piso piso = buscarPiso(pisoId);
        Edificio edificio = buscarEdificio(piso.getEdificioId());

        if (edificio.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar espacios en un edificio inactivo");
        }

        if (piso.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar espacios en un piso inactivo");
        }

        String codigo = request.codigo().trim();

        if (espacioRepository.existsByEdificioIdAndCodigo(edificio.getId(), codigo)) {
            throw new EspacioDuplicadoException(codigo);
        }

        Espacio espacio = new Espacio();
        espacio.setEdificioId(edificio.getId());
        espacio.setPisoId(pisoId);
        espacio.setCodigo(codigo);
        espacio.setNombre(request.nombre().trim());
        espacio.setTipo(request.tipo());

        Espacio guardado = espacioRepository.save(espacio);
        log.info("Espacio {} creado en el piso {} con código {}",
                guardado.getId(), pisoId, guardado.getCodigo());
        return aRespuesta(guardado, true);
    }

    @Transactional
    public EspacioResponse modificar(UUID id, ModificarEspacioRequest request) {
        Espacio espacio = buscar(id);
        espacio.setNombre(request.nombre().trim());
        espacio.setTipo(request.tipo());

        Espacio guardado = espacioRepository.save(espacio);
        log.info("Espacio {} modificado", guardado.getId());
        return aRespuesta(guardado);
    }

    @Transactional
    public EspacioResponse inactivar(UUID id) {
        Espacio espacio = buscar(id);
        List<UUID> rutasActivas = verificadorRutas.rutasActivasDeEspacio(id);

        if (!rutasActivas.isEmpty()) {
            log.warn("No se inactivó el espacio {}: es origen de rutas activas {}", id, rutasActivas);
            throw new ReglaNegocioException("El espacio es origen de rutas activas " + rutasActivas
                    + ": deben bloquearse antes de inactivarlo");
        }

        espacio.setEstado(Estado.INACTIVO);

        Espacio guardado = espacioRepository.save(espacio);
        log.info("Espacio {} inactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional
    public EspacioResponse reactivar(UUID id) {
        Espacio espacio = buscar(id);

        if (espacio.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("El espacio ya está activo");
        }

        boolean pisoActivo = buscarPiso(espacio.getPisoId()).getEstado() == Estado.ACTIVO;
        boolean edificioActivo = buscarEdificio(espacio.getEdificioId()).getEstado() == Estado.ACTIVO;

        if (!pisoActivo || !edificioActivo) {
            throw new ReglaNegocioException("El piso o el edificio está inactivo: debe reactivarse primero");
        }

        espacio.setEstado(Estado.ACTIVO);

        Espacio guardado = espacioRepository.save(espacio);
        log.info("Espacio {} reactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<EspacioResponse> listarPorPiso(UUID pisoId, boolean incluirInactivos) {
        Piso piso = buscarPiso(pisoId);
        boolean contenedoresActivos = piso.getEstado() == Estado.ACTIVO
                && buscarEdificio(piso.getEdificioId()).getEstado() == Estado.ACTIVO;

        return filtrar(espacioRepository.findByPisoId(pisoId).stream()
                .map(e -> aRespuesta(e, contenedoresActivos)), incluirInactivos);
    }

    @Transactional(readOnly = true)
    public List<EspacioResponse> listarPorEdificio(UUID edificioId, boolean incluirInactivos) {
        boolean edificioActivo = buscarEdificio(edificioId).getEstado() == Estado.ACTIVO;

        Set<UUID> pisosActivos = pisoRepository.findByEdificioIdOrderByNumeroAsc(edificioId).stream()
                .filter(p -> p.getEstado() == Estado.ACTIVO)
                .map(Piso::getId)
                .collect(Collectors.toSet());

        return filtrar(espacioRepository.findByEdificioId(edificioId).stream()
                .map(e -> aRespuesta(e, edificioActivo && pisosActivos.contains(e.getPisoId()))),
                incluirInactivos);
    }

    @Transactional(readOnly = true)
    public EspacioResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private Espacio buscar(UUID id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new EspacioNoEncontradoException(id));
    }

    private Piso buscarPiso(UUID id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new PisoNoEncontradoException(id));
    }

    private Edificio buscarEdificio(UUID id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private static List<EspacioResponse> filtrar(Stream<EspacioResponse> espacios, boolean incluirInactivos) {
        return espacios.filter(e -> incluirInactivos || e.disponible()).toList();
    }

    private EspacioResponse aRespuesta(Espacio e) {
        boolean pisoActivo = pisoRepository.findById(e.getPisoId())
                .map(p -> p.getEstado() == Estado.ACTIVO)
                .orElse(false);

        boolean edificioActivo = edificioRepository.findById(e.getEdificioId())
                .map(ed -> ed.getEstado() == Estado.ACTIVO)
                .orElse(false);

        return aRespuesta(e, pisoActivo && edificioActivo);
    }

    private static EspacioResponse aRespuesta(Espacio e, boolean contenedoresActivos) {
        return new EspacioResponse(
                e.getId(),
                e.getEdificioId(),
                e.getPisoId(),
                e.getCodigo(),
                e.getNombre(),
                e.getTipo(),
                e.getEstado(),
                contenedoresActivos && e.getEstado() == Estado.ACTIVO
        );
    }
}