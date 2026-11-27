package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;
import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaNoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// El dominio no importa nada de OSM: aquí se prueba el adaptador contra un servidor simulado, sin red.
class OsmProveedorNavegacionTest {

    private static final String BASE = "https://osm.test/routed-foot";
    private static final String URL_ESPERADA = BASE
            + "/route/v1/foot/-74.042,4.782;-74.04,4.784?overview=simplified&geometries=geojson&steps=true";
    private static final Coordenada ORIGEN = Coordenada.de(4.782, -74.042);
    private static final Coordenada DESTINO = Coordenada.de(4.784, -74.040);

    private static final String RESPUESTA_OK = """
            {"code":"Ok","routes":[{"distance":312.4,"duration":225.1,
              "geometry":{"type":"LineString","coordinates":[[-74.042,4.782],[-74.041,4.783],[-74.04,4.784]]},
              "legs":[{"steps":[
                {"distance":120.0,"duration":86.0,"name":"Calle 1","maneuver":{"type":"depart"}},
                {"distance":192.4,"duration":139.1,"name":"","maneuver":{"type":"turn","modifier":"left"}},
                {"distance":0,"duration":0,"name":"Calle 2","maneuver":{"type":"arrive"}}]}]}],
              "waypoints":[]}""";

    private MockRestServiceServer servidor;
    private OsmProveedorNavegacion proveedor;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        // La barra final de la URL base se ignora.
        proveedor = new OsmProveedorNavegacion(builder.build(), BASE + "/", "foot");
    }

    @Test
    void calcularRuta_enviaLongitudPrimeroYLeeLaRespuesta() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

        RutaCalculada ruta = proveedor.calcularRutaPeatonal(ORIGEN, DESTINO);

        assertThat(ruta.distanciaMetros()).isEqualTo(312.4);
        assertThat(ruta.duracionSegundos()).isEqualTo(225.1);
        assertThat(ruta.geometria()).hasSize(3);
        assertThat(ruta.geometria().get(0)).isEqualTo(ORIGEN);
        assertThat(ruta.geometria().get(2)).isEqualTo(DESTINO);
        assertThat(ruta.pasos()).hasSize(3);
        assertThat(ruta.pasos().get(0).instruccion()).isEqualTo("Inicia el recorrido por Calle 1");
        assertThat(ruta.pasos().get(1).instruccion()).isEqualTo("Gira a la izquierda");
        assertThat(ruta.pasos().get(2).instruccion()).isEqualTo("Has llegado al destino");
        servidor.verify();
    }

    @Test
    void codigoNoRoute_enRespuestaExitosa_lanzaRutaNoEncontrada() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("{\"code\":\"NoRoute\",\"message\":\"Impossible route\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(RutaNoEncontradaException.class);
    }

    @Test
    void codigoNoRoute_conEstado400_lanzaRutaNoEncontrada() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"NoRoute\",\"message\":\"Impossible route\"}"));

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(RutaNoEncontradaException.class);
    }

    @Test
    void rutasVacias_lanzaRutaNoEncontrada() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("{\"code\":\"Ok\",\"routes\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(RutaNoEncontradaException.class);
    }

    @Test
    void codigoDeErrorDelProveedor_lanzaNavegacionNoDisponible() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("{\"code\":\"InvalidQuery\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(NavegacionNoDisponibleException.class)
                .hasMessageContaining("InvalidQuery");
    }

    @Test
    void errorDelServidor_lanzaNavegacionNoDisponible() {
        servidor.expect(requestTo(URL_ESPERADA)).andRespond(withServerError());

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(NavegacionNoDisponibleException.class)
                .hasMessageContaining("500");
    }

    @Test
    void timeout_lanzaNavegacionNoDisponible() {
        servidor.expect(requestTo(URL_ESPERADA)).andRespond(request -> {
            throw new SocketTimeoutException("timeout");
        });

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(NavegacionNoDisponibleException.class);
    }

    @Test
    void respuestaQueNoEsJson_lanzaNavegacionNoDisponible() {
        servidor.expect(requestTo(URL_ESPERADA))
                .andRespond(withSuccess("esto no es json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> proveedor.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(NavegacionNoDisponibleException.class);
    }

    @Test
    void describir_traduceLasManiobras() {
        assertThat(OsmProveedorNavegacion.describir(new OsmProveedorNavegacion.Maniobra("turn", "sharp right"), null))
                .isEqualTo("Gira cerrado a la derecha");
        assertThat(OsmProveedorNavegacion.describir(new OsmProveedorNavegacion.Maniobra("end of road", "left"), "Av 9"))
                .isEqualTo("Al final del camino gira a la izquierda por Av 9");
        assertThat(OsmProveedorNavegacion.describir(new OsmProveedorNavegacion.Maniobra("turn", null), ""))
                .isEqualTo("Gira");
        assertThat(OsmProveedorNavegacion.describir(new OsmProveedorNavegacion.Maniobra("fork", "left"), "Av 9"))
                .isEqualTo("Continúa por Av 9");
        assertThat(OsmProveedorNavegacion.describir(null, null)).isEqualTo("Continúa");
    }
}
