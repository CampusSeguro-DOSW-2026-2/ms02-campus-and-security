package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;

import edu.eci.dosw.campusseguro.campus.ruta.port.ProveedorNavegacion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NavegacionConfigTest {

    private final ApplicationContextRunner contexto =
            new ApplicationContextRunner().withUserConfiguration(NavegacionConfig.class);

    @Test
    void sinConfiguracion_usaOpenStreetMapConPerfilPeatonal() {
        contexto.run(ctx -> {
            assertThat(ctx).hasSingleBean(ProveedorNavegacion.class);
            assertThat(ctx.getBean(ProveedorNavegacion.class)).isInstanceOf(OsmProveedorNavegacion.class);

            NavegacionProperties propiedades = ctx.getBean(NavegacionProperties.class);
            assertThat(propiedades.baseUrl()).isEqualTo("https://routing.openstreetmap.de/routed-foot");
            assertThat(propiedades.profile()).isEqualTo("foot");
            assertThat(propiedades.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
            assertThat(propiedades.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        });
    }

    @Test
    void conPropiedades_cambiaDeServidorSinTocarCodigo() {
        contexto.withPropertyValues(
                        "campusseguro.navigation.base-url=https://osrm.interno/foot",
                        "campusseguro.navigation.read-timeout=2s")
                .run(ctx -> {
                    NavegacionProperties propiedades = ctx.getBean(NavegacionProperties.class);
                    assertThat(propiedades.baseUrl()).isEqualTo("https://osrm.interno/foot");
                    assertThat(propiedades.readTimeout()).isEqualTo(Duration.ofSeconds(2));
                });
    }

    private static final String RESPUESTA_OSRM = "{\"code\":\"Ok\",\"routes\":[{\"distance\":184.2,\"duration\":132.6,"
            + "\"geometry\":{\"coordinates\":[[-74.04263,4.78271],[-74.0421,4.78365]]},"
            + "\"legs\":[{\"steps\":[{\"distance\":184.2,\"duration\":132.6,\"name\":\"\","
            + "\"maneuver\":{\"type\":\"depart\"}}]}]}]}";

    // Imita al servidor público de OSRM: si se le pide compresión, responde gzip etiquetado como deflate.
    private static void responderComoOsrm(HttpExchange intercambio) throws IOException {
        byte[] json = RESPUESTA_OSRM.getBytes(StandardCharsets.UTF_8);
        byte[] cuerpo = json;
        String pedida = intercambio.getRequestHeaders().getFirst("Accept-Encoding");
        if (pedida != null && !pedida.isBlank() && !"identity".equals(pedida)) {
            ByteArrayOutputStream gzip = new ByteArrayOutputStream();
            try (GZIPOutputStream salida = new GZIPOutputStream(gzip)) {
                salida.write(json);
            }
            cuerpo = gzip.toByteArray();
            intercambio.getResponseHeaders().add("Content-Encoding", "deflate");
        }
        intercambio.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        intercambio.sendResponseHeaders(200, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    @Test
    void conUnServidorQueEtiquetaMalLaCompresion_igualLeeLaRuta() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", NavegacionConfigTest::responderComoOsrm);
        servidor.start();
        try {
            contexto.withPropertyValues("campusseguro.navigation.base-url=http://localhost:"
                            + servidor.getAddress().getPort() + "/routed-foot")
                    .run(ctx -> {
                        RutaCalculada ruta = ctx.getBean(ProveedorNavegacion.class)
                                .calcularRutaPeatonal(Coordenada.de(4.78271, -74.04263), Coordenada.de(4.78365, -74.0421));
                        assertThat(ruta.distanciaMetros()).isEqualTo(184.2);
                        assertThat(ruta.pasos()).hasSize(1);
                    });
        } finally {
            servidor.stop(0);
        }
    }
}
