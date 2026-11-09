package edu.eci.dosw.campusseguro.campus.service;

import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import edu.eci.dosw.campusseguro.campus.repository.ImagenRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.dto.ModificarSalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalidaService {

    private final ImagenRepository imagenRepository;

    private final SalidaRepository salidaRepository;
    private final PisoRepository pisoRepository;
    private final EdificioRepository edificioRepository;

    @Transactional
    public SalidaResponse crear(UUID edificioId, SalidaRequest request) {
        Edificio edificio = buscarEdificio(edificioId);

        if (edificio.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden registrar salidas en un edificio inactivo");
        }

        Piso piso = pisoActivoDelEdificio(request.pisoId(), edificioId);
        Salida salida = new Salida();
        salida.setEdificioId(edificioId);
        salida.setPisoId(piso.getId());
        salida.setNombre(request.nombre().trim());
        salida.setLatitud(request.latitud());
        salida.setLongitud(request.longitud());

        Salida guardada = salidaRepository.save(salida);
        log.info("Salida {} creada en el edificio {} (piso {})",
                guardada.getId(), edificioId, piso.getId());
        return aRespuesta(guardada, piso, edificio);
    }

    @Transactional
    public SalidaResponse modificar(UUID id, ModificarSalidaRequest request) {
        Salida salida = buscar(id);

        if (!request.pisoId().equals(salida.getPisoId())) {
            salida.setPisoId(pisoActivoDelEdificio(request.pisoId(), salida.getEdificioId()).getId());
        }

        salida.setNombre(request.nombre().trim());
        salida.setLatitud(request.latitud());
        salida.setLongitud(request.longitud());

        Salida guardada = salidaRepository.save(salida);
        log.info("Salida {} modificada", guardada.getId());
        return aRespuesta(guardada);
    }

    @Transactional
    public SalidaResponse inactivar(UUID id) {
        Salida salida = buscar(id);

        if (salida.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("La salida ya está inactiva");
        }

        salida.setEstado(Estado.INACTIVO);

        Salida guardada = salidaRepository.save(salida);
        log.info("Salida {} inactivada", id);
        return aRespuesta(guardada);
    }

    @Transactional
    public SalidaResponse reactivar(UUID id) {
        Salida salida = buscar(id);

        if (salida.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("La salida ya está activa");
        }

        boolean pisoActivo = buscarPiso(salida.getPisoId()).getEstado() == Estado.ACTIVO;
        boolean edificioActivo = buscarEdificio(salida.getEdificioId()).getEstado() == Estado.ACTIVO;

        if (!pisoActivo || !edificioActivo) {
            throw new ReglaNegocioException("El piso o el edificio está inactivo: debe reactivarse primero");
        }

        salida.setEstado(Estado.ACTIVO);

        Salida guardada = salidaRepository.save(salida);
        log.info("Salida {} reactivada", id);
        return aRespuesta(guardada);
    }

    @Transactional(readOnly = true)
    public List<SalidaResponse> listarPorEdificio(UUID edificioId, boolean incluirInactivos) {
        Edificio edificio = buscarEdificio(edificioId);

        Map<UUID, Piso> pisos = pisoRepository.findByEdificioIdOrderByNumeroAsc(edificioId).stream()
                .collect(Collectors.toMap(Piso::getId, Function.identity()));

        return salidaRepository.findByEdificioIdOrderByNombreAsc(edificioId).stream()
                .map(s -> aRespuesta(s, pisos.get(s.getPisoId()), edificio))
                .filter(s -> incluirInactivos || s.disponible())
                .toList();
    }

    @Transactional(readOnly = true)
    public SalidaResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private Piso pisoActivoDelEdificio(UUID pisoId, UUID edificioId) {
        Piso piso = buscarPiso(pisoId);

        if (!piso.getEdificioId().equals(edificioId)) {
            throw new ReglaNegocioException("El piso no pertenece al edificio de la salida");
        }

        if (piso.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se puede asociar una salida a un piso inactivo");
        }

        return piso;
    }

    private Salida buscar(UUID id) {
        return salidaRepository.findById(id)
                .orElseThrow(() -> new SalidaNoEncontradaException(id));
    }

    private Piso buscarPiso(UUID id) {
        return pisoRepository.findById(id)
                .orElseThrow(() -> new PisoNoEncontradoException(id));
    }

    private Edificio buscarEdificio(UUID id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private SalidaResponse aRespuesta(Salida s) {
        return aRespuesta(s, pisoRepository.findById(s.getPisoId()).orElse(null),
                edificioRepository.findById(s.getEdificioId()).orElse(null));
    }

    private SalidaResponse aRespuesta(Salida s, Piso piso, Edificio edificio) {
        boolean disponible = s.getEstado() == Estado.ACTIVO
                && piso != null && piso.getEstado() == Estado.ACTIVO
                && edificio != null && edificio.getEstado() == Estado.ACTIVO;

        return new SalidaResponse(
                s.getId(),
                s.getEdificioId(),
                edificio == null ? null : edificio.getNombre(),
                s.getPisoId(),
                piso == null ? null : piso.getNumero(),
                s.getNombre(),
                s.getLatitud(),
                s.getLongitud(),
                s.getEstado(),
                disponible,
                imagenRepository.existsByTipoElementoAndElementoId(TipoElementoImagen.SALIDA, s.getId())
        );
    }
}
