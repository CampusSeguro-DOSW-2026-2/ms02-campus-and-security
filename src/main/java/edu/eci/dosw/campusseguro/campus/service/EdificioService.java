package edu.eci.dosw.campusseguro.campus.service;

import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import edu.eci.dosw.campusseguro.campus.repository.ImagenRepository;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EdificioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEdificioRequest;
import edu.eci.dosw.campusseguro.campus.entity.Campus;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.CodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.CampusRepository;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdificioService {

    private final ImagenRepository imagenRepository;

    private final EdificioRepository edificioRepository;
    private final VerificadorRutas verificadorRutas;
    private final CampusRepository campusRepository;

    @Transactional
    public EdificioResponse crear(EdificioRequest request) {
        String codigo = request.codigo().trim();

        if (edificioRepository.existsByCodigo(codigo)) {
            throw new CodigoDuplicadoException(codigo);
        }

        validarCampus(request.campusId());

        Edificio edificio = new Edificio();
        edificio.setCodigo(codigo);
        edificio.setCampusId(request.campusId());
        edificio.setNombre(request.nombre().trim());
        edificio.setDescripcion(request.descripcion());

        Edificio guardado = edificioRepository.save(edificio);
        log.info("Edificio {} creado con código {}", guardado.getId(), guardado.getCodigo());
        return aRespuesta(guardado);
    }

    @Transactional
    public EdificioResponse modificar(UUID id, ModificarEdificioRequest request) {
        Edificio edificio = buscar(id);

        // Sin campusId en la solicitud, el campus actual no cambia.
        if (request.campusId() != null && !request.campusId().equals(edificio.getCampusId())) {
            validarCampus(request.campusId());
            edificio.setCampusId(request.campusId());
        }

        edificio.setNombre(request.nombre().trim());
        edificio.setDescripcion(request.descripcion());

        Edificio guardado = edificioRepository.save(edificio);
        log.info("Edificio {} modificado", guardado.getId());
        return aRespuesta(guardado);
    }

    @Transactional
    public EdificioResponse inactivar(UUID id) {
        Edificio edificio = buscar(id);

        List<UUID> rutasActivas =
                verificadorRutas.rutasActivasDeEdificio(id);

        if (!rutasActivas.isEmpty()) {
            log.warn("No se inactivó el edificio {}: tiene rutas activas {}", id, rutasActivas);
            throw new ReglaNegocioException(
                    "El edificio tiene rutas activas " + rutasActivas
                            + ": deben bloquearse antes de inactivarlo");
        }

        edificio.setEstado(Estado.INACTIVO);

        Edificio guardado = edificioRepository.save(edificio);
        log.info("Edificio {} inactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional
    public EdificioResponse reactivar(UUID id) {
        Edificio edificio = buscar(id);

        if (edificio.getEstado() == Estado.ACTIVO) {
            throw new ReglaNegocioException("El edificio ya está activo");
        }

        edificio.setEstado(Estado.ACTIVO);

        Edificio guardado = edificioRepository.save(edificio);
        log.info("Edificio {} reactivado", id);
        return aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<EdificioResponse> listar(boolean incluirInactivos) {
        return edificioRepository.findAll().stream()
                .filter(e -> incluirInactivos || e.getEstado() == Estado.ACTIVO)
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EdificioResponse obtener(UUID id) {
        return aRespuesta(buscar(id));
    }

    private void validarCampus(UUID campusId) {
        if (campusId == null) {
            return;
        }
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new CampusNoEncontradoException(campusId));
        if (campus.getEstado() == Estado.INACTIVO) {
            throw new ReglaNegocioException("No se pueden asignar edificios a un campus inactivo");
        }
    }

    private Edificio buscar(UUID id) {
        return edificioRepository.findById(id)
                .orElseThrow(() -> new EdificioNoEncontradoException(id));
    }

    private EdificioResponse aRespuesta(Edificio e) {
        return new EdificioResponse(
                e.getId(),
                e.getCodigo(),
                e.getNombre(),
                e.getDescripcion(),
                e.getEstado(),
                e.getCampusId(),
                imagenRepository.existsByTipoElementoAndElementoId(TipoElementoImagen.EDIFICIO, e.getId()));
    }
}