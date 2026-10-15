package co.edu.escuelaing.campusseguro.campus.ruta.service;

import co.edu.escuelaing.campusseguro.campus.ruta.domain.EstadoRuta;
import co.edu.escuelaing.campusseguro.campus.ruta.domain.RutaEvacuacion;
import co.edu.escuelaing.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import co.edu.escuelaing.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import co.edu.escuelaing.campusseguro.campus.ruta.exception.ReglaNegocioException;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EdificioGateway;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EspacioGateway;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import co.edu.escuelaing.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.repository.RutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ConsultaRutaService {

    static final String MSG_SIN_RUTA = "No existe una ruta recomendada para la selección.";
    static final String MSG_BLOQUEADA = "La ruta está bloqueada. No la use y siga las indicaciones del personal de emergencia.";
    static final String MSG_RESTRINGIDA = "La ruta está restringida. Úsela solo si el personal de emergencia lo indica.";
    static final String MSG_DESTINO_INACTIVO = "La zona segura de destino está inactiva. Consulte al personal de emergencia.";

    private final RutaRepository rutaRepository;
    private final EdificioGateway edificioGateway;
    private final EspacioGateway espacioGateway;
    private final ZonaSeguraGateway zonaGateway;

    public ConsultaRutaService(RutaRepository rutaRepository,
                               EdificioGateway edificioGateway,
                               EspacioGateway espacioGateway,
                               ZonaSeguraGateway zonaGateway) {
        this.rutaRepository = rutaRepository;
        this.edificioGateway = edificioGateway;
        this.espacioGateway = espacioGateway;
        this.zonaGateway = zonaGateway;
    }

    public RutaConsultadaResponse consultarPorEspacio(Long espacioId) {
        EspacioInfo espacio = espacioGateway.buscarPorId(espacioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El espacio " + espacioId + " no existe"));
        return consultar(espacio.edificioId(), espacioId).get(0);
    }

    public List<RutaConsultadaResponse> consultarPorEdificio(Long edificioId) {
        return consultar(edificioId, null);
    }

    public List<RutaConsultadaResponse> consultar(Long edificioId, Long espacioId) {
        EdificioInfo edificio = edificioGateway.buscarPorId(edificioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El edificio " + edificioId + " no existe"));
        if (!edificio.activo()) {
            throw new ReglaNegocioException("El edificio '" + edificio.nombre() + "' está inactivo");
        }

        if (espacioId != null) {
            EspacioInfo espacio = espacioGateway.buscarPorId(espacioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El espacio " + espacioId + " no existe"));
            if (!espacio.edificioId().equals(edificioId)) {
                throw new ReglaNegocioException(
                        "El espacio '" + espacio.nombre() + "' no pertenece al edificio seleccionado");
            }
            Optional<RutaEvacuacion> ruta = rutaRepository.findByEspacioOrigenId(espacioId);
            return List.of(armar(espacio, ruta.orElse(null)));
        }

        List<EspacioInfo> espacios = espacioGateway.listarPorEdificio(edificioId).stream()
                .filter(EspacioInfo::activo)
                .toList();
        if (espacios.isEmpty()) {
            return List.of();
        }
        Map<Long, RutaEvacuacion> rutasPorEspacio = rutaRepository
                .findByEspacioOrigenIdIn(espacios.stream().map(EspacioInfo::id).toList())
                .stream()
                .collect(Collectors.toMap(RutaEvacuacion::getEspacioOrigenId, Function.identity()));
        return espacios.stream()
                .map(e -> armar(e, rutasPorEspacio.get(e.id())))
                .toList();
    }

    private RutaConsultadaResponse armar(EspacioInfo espacio, RutaEvacuacion ruta) {
        if (ruta == null) {
            return new RutaConsultadaResponse(espacio.id(), espacio.nombre(), false,
                    null, null, null, null, null, false, MSG_SIN_RUTA);
        }

        Optional<ZonaInfo> zona = zonaGateway.buscarPorId(ruta.getZonaSeguraDestinoId());
        boolean destinoActivo = zona.map(ZonaInfo::activa).orElse(false);

        return new RutaConsultadaResponse(
                espacio.id(),
                espacio.nombre(),
                true,
                ruta.getZonaSeguraDestinoId(),
                zona.map(ZonaInfo::nombre).orElse(null),
                zona.map(ZonaInfo::ubicacion).orElse(null),
                ruta.getDescripcion(),
                ruta.getEstado(),
                destinoActivo,
                mensajeDe(ruta.getEstado(), destinoActivo));
    }

    // Si el destino esta inactivo, ese aviso va primero.
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
