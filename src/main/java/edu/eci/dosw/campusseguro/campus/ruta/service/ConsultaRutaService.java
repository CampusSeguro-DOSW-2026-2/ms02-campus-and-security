
package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import edu.eci.dosw.campusseguro.campus.repository.RecomendacionPreventivaRepository;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.RecomendacionPreventiva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultaRutaService {

    static final String MSG_SIN_RUTA =
            "No existe una ruta recomendada para la selección.";

    static final String MSG_BLOQUEADA =
            "La ruta está bloqueada. No la use y siga las indicaciones del personal de emergencia.";

    static final String MSG_RESTRINGIDA =
            "La ruta está restringida. Úsela solo si el personal de emergencia lo indica.";

    static final String MSG_DESTINO_INACTIVO =
            "La zona segura de destino está inactiva. Consulte al personal de emergencia.";

    private final RutaRepository rutaRepository;
    private final EdificioGateway edificioGateway;
    private final EspacioGateway espacioGateway;
    private final ZonaSeguraGateway zonaGateway;

    private final RecomendacionPreventivaRepository recomendacionRepository;

    // Constructor utilizado por Spring en la aplicación.
    @Autowired
    public ConsultaRutaService(
            RutaRepository rutaRepository,
            EdificioGateway edificioGateway,
            EspacioGateway espacioGateway,
            ZonaSeguraGateway zonaGateway,
            RecomendacionPreventivaRepository recomendacionRepository) {
        this.rutaRepository = rutaRepository;
        this.edificioGateway = edificioGateway;
        this.espacioGateway = espacioGateway;
        this.zonaGateway = zonaGateway;
        this.recomendacionRepository = recomendacionRepository;
    }

    // Mantiene compatibilidad con las pruebas existentes.
    public ConsultaRutaService(
            RutaRepository rutaRepository,
            EdificioGateway edificioGateway,
            EspacioGateway espacioGateway,
            ZonaSeguraGateway zonaGateway) {
        this(rutaRepository, edificioGateway, espacioGateway, zonaGateway, null);
    }

    public RutaConsultadaResponse consultarPorEspacio(UUID espacioId) {
        EspacioInfo espacio = espacioGateway.buscarPorId(espacioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El espacio " + espacioId + " no existe"));

        if (!espacio.disponible()) {
            throw new ReglaNegocioException(
                    "El espacio '" + espacio.nombre()
                            + "' no está disponible");
        }

        return consultar(espacio.edificioId(), espacioId).get(0);
    }

    public List<RutaConsultadaResponse> consultarPorEdificio(UUID edificioId) {
        return consultar(edificioId, null);
    }

    public List<RutaConsultadaResponse> consultar(UUID edificioId, UUID espacioId) {
        EdificioInfo edificio = edificioGateway.buscarPorId(edificioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El edificio " + edificioId + " no existe"));

        if (!edificio.activo()) {
            throw new ReglaNegocioException(
                    "El edificio '" + edificio.nombre() + "' está inactivo");
        }

        if (espacioId != null) {
            EspacioInfo espacio = espacioGateway.buscarPorId(espacioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El espacio " + espacioId + " no existe"));

            if (!espacio.edificioId().equals(edificioId)) {
                throw new ReglaNegocioException(
                        "El espacio '" + espacio.nombre()
                                + "' no pertenece al edificio seleccionado");
            }

            if (!espacio.disponible()) {
                throw new ReglaNegocioException(
                        "El espacio '" + espacio.nombre()
                                + "' no está disponible");
            }

            Optional<RutaEvacuacion> ruta =
                    rutaRepository.findByEspacioOrigenId(espacioId);

            return List.of(armar(espacio, ruta.orElse(null)));
        }

        List<EspacioInfo> espacios = espacioGateway
                .listarPorEdificio(edificioId)
                .stream()
                .filter(EspacioInfo::disponible)
                .toList();

        if (espacios.isEmpty()) {
            return List.of();
        }

        Map<UUID, RutaEvacuacion> rutasPorEspacio = rutaRepository
                .findByEspacioOrigenIdIn(
                        espacios.stream()
                                .map(EspacioInfo::id)
                                .toList())
                .stream()
                .collect(Collectors.toMap(
                        RutaEvacuacion::getEspacioOrigenId,
                        Function.identity()));

        return espacios.stream()
                .map(espacio -> armar(
                        espacio, rutasPorEspacio.get(espacio.id())))
                .toList();
    }

    private RutaConsultadaResponse armar(
            EspacioInfo espacio, RutaEvacuacion ruta) {
        if (ruta == null) {
            return new RutaConsultadaResponse(
                    espacio.id(),
                    espacio.nombre(),
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    MSG_SIN_RUTA,
                    recomendacionesDe(espacio));
        }

        Optional<ZonaInfo> zona =
                zonaGateway.buscarPorId(ruta.getZonaSeguraDestinoId());

        boolean destinoActivo = zona
                .map(ZonaInfo::activa)
                .orElse(false);

        return new RutaConsultadaResponse(
                espacio.id(),
                espacio.nombre(),
                true,
                ruta.getZonaSeguraDestinoId(),
                zona.map(ZonaInfo::nombre).orElse(null),
                zona.map(ZonaInfo::ubicacion).orElse(null),
                ruta.getDescripcion(),
                ruta.getEstado(),
                ruta.getMotivo(),
                destinoActivo,
                mensajeDe(ruta.getEstado(), destinoActivo),
                recomendacionesDe(espacio));
    }

    private List<RecomendacionPreventivaResponse> recomendacionesDe(EspacioInfo espacio) {
        if (recomendacionRepository == null) {
            return List.of();
        }

        List<RecomendacionPreventivaResponse> resultado = new java.util.ArrayList<>();

        recomendacionRepository
                .findByEstadoAndEdificioId(Estado.ACTIVO, espacio.edificioId())
                .stream()
                .map(this::aRecomendacionResponse)
                .forEach(resultado::add);

        if (espacio.tipoEspacio() != null) {
            recomendacionRepository
                    .findByEstadoAndTipoEspacio(Estado.ACTIVO, espacio.tipoEspacio())
                    .stream()
                    .map(this::aRecomendacionResponse)
                    .forEach(resultado::add);
        }

        return List.copyOf(resultado);
    }

    private RecomendacionPreventivaResponse aRecomendacionResponse(
            RecomendacionPreventiva entidad) {
        return new RecomendacionPreventivaResponse(
                entidad.getId(), entidad.getAmbito(), entidad.getEdificioId(),
                entidad.getTipoEspacio(), entidad.getTitulo(), entidad.getRecomendacion(),
                entidad.getEstado(), entidad.getCreatedAt(), entidad.getUpdatedAt());
    }

    // Si el destino está inactivo, ese aviso va primero.
    private String mensajeDe(EstadoRuta estado, boolean destinoActivo) {
        if (!destinoActivo) {
            return MSG_DESTINO_INACTIVO;
        }

        return switch (estado) {
            case BLOQUEADA -> MSG_BLOQUEADA;
            case RESTRINGIDA -> MSG_RESTRINGIDA;
            case DISPONIBLE -> null;
        };
    }
}
