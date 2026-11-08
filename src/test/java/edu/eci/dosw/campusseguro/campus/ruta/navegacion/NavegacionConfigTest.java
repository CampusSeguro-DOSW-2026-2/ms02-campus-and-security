package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

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
}
