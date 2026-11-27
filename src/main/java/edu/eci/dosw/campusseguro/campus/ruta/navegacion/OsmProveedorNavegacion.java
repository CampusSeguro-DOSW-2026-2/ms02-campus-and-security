package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.PasoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;
import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.ruta.port.ProveedorNavegacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// Adaptador de OpenStreetMap: consulta un servidor OSRM con perfil peatonal. Es el único lugar que conoce ese formato.
public class OsmProveedorNavegacion implements ProveedorNavegacion {

    private static final Logger log = LoggerFactory.getLogger(OsmProveedorNavegacion.class);
    private static final String SIN_RUTA = "NoRoute";
    private static final Map<String, String> MODIFICADORES = Map.of(
            "left", "a la izquierda",
            "right", "a la derecha",
            "slight left", "ligeramente a la izquierda",
            "slight right", "ligeramente a la derecha",
            "sharp left", "cerrado a la izquierda",
            "sharp right", "cerrado a la derecha",
            "straight", "de frente",
            "uturn", "en U");

    private final RestClient restClient;
    private final String baseUrl;
    private final String perfil;

    public OsmProveedorNavegacion(RestClient restClient, String baseUrl, String perfil) {
        this.restClient = restClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.perfil = perfil;
    }

    @Override
    public RutaCalculada calcularRutaPeatonal(Coordenada origen, Coordenada destino) {
        URI uri = construirUri(origen, destino);
        RespuestaOsrm respuesta;
        try {
            respuesta = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RespuestaOsrm.class);
        } catch (RestClientResponseException e) {
            if (e.getResponseBodyAsString().contains(SIN_RUTA)) {
                throw new RutaNoEncontradaException();
            }
            log.warn("El servicio de navegación respondió {}", e.getStatusCode().value());
            throw new NavegacionNoDisponibleException(
                    "El servicio de navegación respondió con el estado " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            log.warn("No se pudo consultar el servicio de navegación: {}", e.getMessage());
            throw new NavegacionNoDisponibleException("No se pudo consultar el servicio de navegación", e);
        }
        return aRuta(respuesta);
    }

    // OSRM recibe longitud,latitud (en ese orden). La URI se arma completa para no re-codificar ',' ni ';'.
    private URI construirUri(Coordenada origen, Coordenada destino) {
        String coordenadas = String.format(Locale.ROOT, "%s,%s;%s,%s",
                texto(origen.longitud()), texto(origen.latitud()),
                texto(destino.longitud()), texto(destino.latitud()));
        return URI.create(baseUrl + "/route/v1/" + perfil + "/" + coordenadas
                + "?overview=simplified&geometries=geojson&steps=true");
    }

    private String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private RutaCalculada aRuta(RespuestaOsrm respuesta) {
        if (respuesta == null || respuesta.code() == null) {
            throw new NavegacionNoDisponibleException("El servicio de navegación devolvió una respuesta vacía");
        }
        if (SIN_RUTA.equals(respuesta.code())) {
            throw new RutaNoEncontradaException();
        }
        if (!"Ok".equals(respuesta.code())) {
            throw new NavegacionNoDisponibleException(
                    "El servicio de navegación devolvió el código " + respuesta.code());
        }
        if (respuesta.routes() == null || respuesta.routes().isEmpty()) {
            throw new RutaNoEncontradaException();
        }

        Ruta ruta = respuesta.routes().get(0);
        List<Coordenada> geometria = ruta.geometry() == null || ruta.geometry().coordinates() == null
                ? List.of()
                : ruta.geometry().coordinates().stream()
                        .filter(punto -> punto.size() >= 2)
                        .map(punto -> Coordenada.de(punto.get(1), punto.get(0)))
                        .toList();
        List<PasoRuta> pasos = ruta.legs() == null
                ? List.of()
                : ruta.legs().stream()
                        .filter(tramo -> tramo.steps() != null)
                        .flatMap(tramo -> tramo.steps().stream())
                        .map(paso -> new PasoRuta(
                                describir(paso.maneuver(), paso.name()), paso.distance(), paso.duration()))
                        .toList();

        return new RutaCalculada(ruta.distance(), ruta.duration(), geometria, pasos);
    }

    static String describir(Maniobra maniobra, String calle) {
        String tipo = maniobra == null || maniobra.type() == null ? "" : maniobra.type();
        // Map.of lanza NullPointerException con get(null), por eso se revisa antes.
        String modificador = maniobra == null || maniobra.modifier() == null
                ? null : MODIFICADORES.get(maniobra.modifier());

        String texto = switch (tipo) {
            case "depart" -> "Inicia el recorrido";
            case "arrive" -> "Has llegado al destino";
            case "turn" -> modificador == null ? "Gira" : "Gira " + modificador;
            case "end of road" -> modificador == null ? "Al final del camino gira" : "Al final del camino gira " + modificador;
            default -> "Continúa";
        };
        boolean llegada = "arrive".equals(tipo);
        return calle == null || calle.isBlank() || llegada ? texto : texto + " por " + calle;
    }

    // Formato de la respuesta de OSRM. Solo se leen los campos necesarios; el resto se ignora.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaOsrm(String code, String message, List<Ruta> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ruta(double distance, double duration, Geometria geometry, List<Tramo> legs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Geometria(List<List<Double>> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Tramo(List<Paso> steps) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Paso(double distance, double duration, String name, Maniobra maneuver) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Maniobra(String type, String modifier) {
    }
}
