package edu.eci.dosw.campusseguro.campus.service;

import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaRequest;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;
import edu.eci.dosw.campusseguro.campus.entity.AmbitoRecomendacion;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.RecomendacionPreventiva;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.RecomendacionPreventivaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RecomendacionPreventivaService {

    private final RecomendacionPreventivaRepository repository;
    private final EdificioRepository edificioRepository;

    public RecomendacionPreventivaService(
            RecomendacionPreventivaRepository repository,
            EdificioRepository edificioRepository) {
        this.repository = repository;
        this.edificioRepository = edificioRepository;
    }

    @Transactional(readOnly = true)
    public List<RecomendacionPreventivaResponse> listarActivas() {
        return repository.findByEstado(Estado.ACTIVO)
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<RecomendacionPreventivaResponse> consultarPorEdificio(UUID edificioId) {
        if (!edificioRepository.existsById(edificioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El edificio no existe");
        }

        return repository.findByEstadoAndEdificioId(Estado.ACTIVO, edificioId)
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<RecomendacionPreventivaResponse> consultarPorTipo(
            edu.eci.dosw.campusseguro.campus.entity.TipoEspacio tipo) {
        return repository.findByEstadoAndTipoEspacio(Estado.ACTIVO, tipo)
                .stream().map(this::respuesta).toList();
    }

    public RecomendacionPreventivaResponse crear(RecomendacionPreventivaRequest request) {
        validar(request);

        RecomendacionPreventiva entidad = new RecomendacionPreventiva();
        aplicar(entidad, request);
        entidad.setEstado(Estado.ACTIVO);

        return respuesta(repository.save(entidad));
    }

    public RecomendacionPreventivaResponse modificar(
            UUID id, RecomendacionPreventivaRequest request) {
        validar(request);

        RecomendacionPreventiva entidad = buscar(id);
        aplicar(entidad, request);
        return respuesta(repository.save(entidad));
    }

    public RecomendacionPreventivaResponse inactivar(UUID id) {
        RecomendacionPreventiva entidad = buscar(id);
        entidad.setEstado(Estado.INACTIVO);
        return respuesta(repository.save(entidad));
    }

    private RecomendacionPreventiva buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La recomendación no existe"));
    }

    private void validar(RecomendacionPreventivaRequest request) {
        if (request.ambito() == AmbitoRecomendacion.EDIFICIO) {
            if (request.edificioId() == null || request.tipoEspacio() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para el ámbito EDIFICIO se requiere edificioId y no tipoEspacio");
            }
            if (!edificioRepository.existsById(request.edificioId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El edificio no existe");
            }
        } else if (request.ambito() == AmbitoRecomendacion.TIPO_ESPACIO) {
            if (request.tipoEspacio() == null || request.edificioId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para TIPO_ESPACIO se requiere tipoEspacio y no edificioId");
            }
        }
    }

    private void aplicar(
            RecomendacionPreventiva entidad, RecomendacionPreventivaRequest request) {
        entidad.setAmbito(request.ambito());
        entidad.setEdificioId(request.edificioId());
        entidad.setTipoEspacio(request.tipoEspacio());
        entidad.setTitulo(request.titulo().trim());
        entidad.setRecomendacion(request.recomendacion().trim());
    }

    private RecomendacionPreventivaResponse respuesta(RecomendacionPreventiva entidad) {
        return new RecomendacionPreventivaResponse(
                entidad.getId(), entidad.getAmbito(), entidad.getEdificioId(),
                entidad.getTipoEspacio(), entidad.getTitulo(), entidad.getRecomendacion(),
                entidad.getEstado(), entidad.getCreatedAt(), entidad.getUpdatedAt());
    }
}
