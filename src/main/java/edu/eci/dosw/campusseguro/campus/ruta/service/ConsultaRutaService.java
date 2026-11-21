
package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.GuiaEvacuacionResponse;
import edu.eci.dosw.campusseguro.campus.ruta.dto.GuiaEvacuacionResponse.EscaleraGuia;
import edu.eci.dosw.campusseguro.campus.ruta.dto.GuiaEvacuacionResponse.SalidaGuia;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaExteriorResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EscaleraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EscaleraGateway.EscaleraInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
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

    static final String MSG_SIN_SALIDA =
            "El edificio no tiene una salida con ruta exterior disponible. Siga las indicaciones del personal de emergencia.";

    static final String MSG_SIN_ESCALERA =
            "No hay una escalera disponible para bajar desde este piso. Siga las indicaciones del personal de emergencia.";

    static final String MSG_DESTINO_INACTIVO =
            "La zona segura de destino está inactiva. Consulte al personal de emergencia.";

    private final RutaRepository rutaRepository;
    private final EdificioGateway edificioGateway;
    private final EspacioGateway espacioGateway;
    private final ZonaSeguraGateway zonaGateway;

    private final RecomendacionPreventivaRepository recomendacionRepository;
    private final SalidaGateway salidaGateway;
    private final EscaleraGateway escaleraGateway;

    // Constructor utilizado por Spring en la aplicación.
    @Autowired
    public ConsultaRutaService(
            RutaRepository rutaRepository,
            EdificioGateway edificioGateway,
            EspacioGateway espacioGateway,
            ZonaSeguraGateway zonaGateway,
            RecomendacionPreventivaRepository recomendacionRepository,
            SalidaGateway salidaGateway,
            EscaleraGateway escaleraGateway) {
        this.rutaRepository = rutaRepository;
        this.edificioGateway = edificioGateway;
        this.espacioGateway = espacioGateway;
        this.zonaGateway = zonaGateway;
        this.recomendacionRepository = recomendacionRepository;
        this.salidaGateway = salidaGateway;
        this.escaleraGateway = escaleraGateway;
    }

    // Sin guía de evacuación (salidas y escaleras); mantiene compatibilidad con las pruebas existentes.
    public ConsultaRutaService(
            RutaRepository rutaRepository,
            EdificioGateway edificioGateway,
            EspacioGateway espacioGateway,
            ZonaSeguraGateway zonaGateway,
            RecomendacionPreventivaRepository recomendacionRepository) {
        this(rutaRepository, edificioGateway, espacioGateway, zonaGateway, recomendacionRepository, null, null);
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

        edificioActivo(espacio.edificioId());

        return resumenDesdeGuia(espacio, guiaSiPosible(espacio));
    }

    // La guía solo se puede calcular si el servicio tiene con qué (salidas y escaleras); si no,
    // se mantiene el comportamiento previo de no traer guía.
    private GuiaEvacuacionResponse guiaSiPosible(EspacioInfo espacio) {
        return (salidaGateway == null || escaleraGateway == null) ? null : armarGuia(espacio);
    }

    private EdificioInfo edificioActivo(UUID edificioId) {
        EdificioInfo edificio = edificioGateway.buscarPorId(edificioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El edificio " + edificioId + " no existe"));

        if (!edificio.activo()) {
            throw new ReglaNegocioException(
                    "El edificio '" + edificio.nombre() + "' está inactivo");
        }
        return edificio;
    }

    // Arma la sección "evacuación" (RF-MS02-14) a partir de la guía automática: la zona segura de la
    // ruta exterior elegida, su indicación, su estado y su motivo. Ya no depende de una ruta manual
    // espacio -> zona, que se dejó de crear (RF-MS02-05).
    private RutaConsultadaResponse resumenDesdeGuia(EspacioInfo espacio, GuiaEvacuacionResponse guia) {
        List<RecomendacionPreventivaResponse> recomendaciones = recomendacionesDe(espacio);

        if (guia == null || guia.salida() == null) {
            String mensaje = (guia != null && guia.mensaje() != null) ? guia.mensaje() : MSG_SIN_RUTA;
            return new RutaConsultadaResponse(
                    espacio.id(), espacio.nombre(), false, null, null, null,
                    null, null, null, false, mensaje, recomendaciones, guia);
        }

        RutaExteriorResponse exterior = guia.rutaExterior();
        String ubicacionZona = zonaGateway.buscarPorId(exterior.zonaSeguraDestinoId())
                .map(ZonaInfo::ubicacion).orElse(null);

        // La guía solo elige rutas hacia zonas activas (ver armarGuia), así que si hay ruta, el destino
        // siempre está activo; lo que puede pasar es que la ruta exterior esté bloqueada o restringida,
        // y eso se avisa con el mismo mensaje que antes, para que quede claro al usuario final.
        return new RutaConsultadaResponse(
                espacio.id(), espacio.nombre(), true,
                exterior.zonaSeguraDestinoId(), guia.zonaSeguraNombre(), ubicacionZona,
                guia.indicacion(), exterior.estado(), exterior.motivo(), true,
                guia.mensaje(), recomendaciones, guia);
    }

    // Guía completa: escalera (solo las DISPONIBLES), salida y ruta exterior. Elige la salida alcanzable
    // con el recorrido exterior más corto; si hay salida en el mismo piso no hace falta escalera.
    private GuiaEvacuacionResponse armarGuia(EspacioInfo espacio) {
        String edificioNombre = edificioGateway.buscarPorId(espacio.edificioId())
                .map(EdificioInfo::nombre).orElse(null);

        List<SalidaInfo> salidas = salidaGateway.listarPorEdificio(espacio.edificioId()).stream()
                .filter(SalidaInfo::disponible)
                .toList();

        // No se descartan aquí las rutas BLOQUEADA/RESTRINGIDA: si es la única opción hacia una zona
        // activa, se recomienda igual más abajo, pero avisando con su estado y su motivo, en vez de
        // decir que no hay ruta cuando en realidad sí existe una, solo que no se puede usar ahora mismo.
        Map<UUID, List<RutaEvacuacion>> rutasPorSalida = salidas.isEmpty() ? Map.of()
                : rutaRepository.findBySalidaOrigenIdIn(salidas.stream().map(SalidaInfo::id).toList())
                        .stream()
                        .filter(r -> zonaGateway.buscarPorId(r.getZonaSeguraDestinoId())
                                .map(ZonaInfo::activa).orElse(false))
                        .collect(Collectors.groupingBy(RutaEvacuacion::getSalidaOrigenId));

        record Opcion(SalidaInfo salida, RutaEvacuacion ruta, List<List<EscaleraInfo>> tramos) {
        }

        List<Opcion> opciones = new ArrayList<>();
        boolean hayRutaSinEscalera = false;
        for (SalidaInfo salida : salidas) {
            List<RutaEvacuacion> rutas = rutasPorSalida.getOrDefault(salida.id(), List.of());
            if (rutas.isEmpty()) {
                continue;
            }
            List<List<EscaleraInfo>> tramos = List.of();
            if (!salida.pisoId().equals(espacio.pisoId())) {
                tramos = escaleraGateway.tramosEntre(
                        espacio.edificioId(), espacio.pisoId(), salida.pisoId());
                if (tramos.isEmpty()) {
                    hayRutaSinEscalera = true;
                    continue;
                }
            }
            // Entre las rutas de esta salida, siempre se prefiere una DISPONIBLE; solo si todas están
            // bloqueadas o restringidas se usa la de recorrido más corto entre esas.
            RutaEvacuacion mejor = rutas.stream()
                    .min(Comparator.comparing((RutaEvacuacion r) -> r.getEstado() != EstadoRuta.DISPONIBLE)
                            .thenComparing(r -> r.getDistanciaMetros() == null
                                    ? java.math.BigDecimal.valueOf(Long.MAX_VALUE) : r.getDistanciaMetros()))
                    .orElseThrow();
            opciones.add(new Opcion(salida, mejor, tramos));
        }

        if (opciones.isEmpty()) {
            String mensaje = hayRutaSinEscalera ? MSG_SIN_ESCALERA : MSG_SIN_SALIDA;
            return new GuiaEvacuacionResponse(espacio.edificioId(), edificioNombre, espacio.pisoId(),
                    espacio.pisoNumero(), hayRutaSinEscalera, List.of(), null, null, null, null, mensaje);
        }

        // Primero cualquier opción con ruta DISPONIBLE (usable), aunque pida más tramos de escalera;
        // solo si ninguna lo está se elige entre las bloqueadas o restringidas. A igual disponibilidad,
        // gana la de menos tramos y, a igual número de tramos, la de recorrido exterior más corto.
        Opcion elegida = opciones.stream()
                .min(Comparator.comparing((Opcion o) -> o.ruta().getEstado() != EstadoRuta.DISPONIBLE)
                        .thenComparingInt(o -> o.tramos().size())
                        .thenComparing(o -> o.ruta().getDistanciaMetros() == null
                                ? java.math.BigDecimal.valueOf(Long.MAX_VALUE) : o.ruta().getDistanciaMetros()))
                .orElseThrow();

        RutaExteriorResponse exterior = RutaExteriorService.aResponse(elegida.ruta());
        String zonaNombre = zonaGateway.buscarPorId(elegida.ruta().getZonaSeguraDestinoId())
                .map(ZonaInfo::nombre).orElse(null);
        // Si la ruta elegida no está disponible, el aviso (bloqueada/restringida) se antepone a la
        // indicación: el usuario ve igual el recorrido, pero sabe que no debe usarlo por su cuenta.
        String avisoEstado = mensajeDe(elegida.ruta().getEstado(), true);
        List<EscaleraGuia> escaleras = new ArrayList<>();
        for (int i = 0; i < elegida.tramos().size(); i++) {
            List<EscaleraInfo> candidatas = elegida.tramos().get(i);
            for (int j = 0; j < candidatas.size(); j++) {
                EscaleraInfo e = candidatas.get(j);
                escaleras.add(new EscaleraGuia(e.id(), e.codigo(), e.nombre(), i + 1, j == 0));
            }
        }
        SalidaInfo salida = elegida.salida();

        return new GuiaEvacuacionResponse(
                espacio.edificioId(), edificioNombre, espacio.pisoId(), espacio.pisoNumero(),
                !escaleras.isEmpty(), escaleras,
                new SalidaGuia(salida.id(), salida.nombre(), salida.latitud(), salida.longitud()),
                exterior, zonaNombre,
                indicacion(escaleras, salida, exterior, zonaNombre), avisoEstado);
    }

    private static String indicacion(List<EscaleraGuia> escaleras, SalidaInfo salida,
                                     RutaExteriorResponse exterior, String zonaNombre) {
        StringBuilder sb = new StringBuilder();
        if (!escaleras.isEmpty()) {
            // Una escalera principal por tramo, en orden; las alternas se mencionan entre parentesis.
            Map<Integer, List<EscaleraGuia>> porTramo = escaleras.stream()
                    .collect(Collectors.groupingBy(EscaleraGuia::tramo, java.util.TreeMap::new, Collectors.toList()));
            sb.append("Baje por la escalera ");
            sb.append(porTramo.values().stream().map(ConsultaRutaService::textoTramo)
                    .collect(Collectors.joining(", luego por la escalera ")));
            sb.append(" hasta el piso de salida, ");
        }
        sb.append(escaleras.isEmpty() ? "Salga" : "salga").append(" por '").append(salida.nombre()).append("'");
        sb.append(" y siga la ruta exterior");
        if (exterior.distanciaMetros() != null) {
            sb.append(" (").append(exterior.distanciaMetros().setScale(0, java.math.RoundingMode.HALF_UP))
                    .append(" m");
            if (exterior.duracionSegundos() != null) {
                sb.append(", unos ")
                        .append(Math.max(1, Math.round(exterior.duracionSegundos().doubleValue() / 60)))
                        .append(" min");
            }
            sb.append(")");
        }
        sb.append(" hasta la zona segura");
        if (zonaNombre != null) {
            sb.append(" '").append(zonaNombre).append("'");
        }
        return sb.append(".").toString();
    }

    private static String textoTramo(List<EscaleraGuia> candidatas) {
        String texto = candidatas.get(0).nombre();
        if (candidatas.size() > 1) {
            texto += " (alterna: " + candidatas.stream().skip(1).map(EscaleraGuia::nombre)
                    .collect(Collectors.joining(", ")) + ")";
        }
        return texto;
    }

    public List<RutaConsultadaResponse> consultarPorEdificio(UUID edificioId) {
        return consultar(edificioId, null);
    }

    public List<RutaConsultadaResponse> consultar(UUID edificioId, UUID espacioId) {
        edificioActivo(edificioId);

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

            return List.of(resumenDesdeGuia(espacio, guiaSiPosible(espacio)));
        }

        List<EspacioInfo> espacios = espacioGateway
                .listarPorEdificio(edificioId)
                .stream()
                .filter(EspacioInfo::disponible)
                .toList();

        if (espacios.isEmpty()) {
            return List.of();
        }

        return espacios.stream()
                .map(espacio -> resumenDesdeGuia(espacio, guiaSiPosible(espacio)))
                .toList();
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
