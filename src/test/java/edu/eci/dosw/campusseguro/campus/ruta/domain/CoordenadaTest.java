package edu.eci.dosw.campusseguro.campus.ruta.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CoordenadaTest {

    @Test
    void de_redondeaASeisDecimales() {
        Coordenada c = Coordenada.de(4.7820004, -74.0420006);

        assertThat(c.latitud()).isEqualByComparingTo("4.782000");
        assertThat(c.longitud()).isEqualByComparingTo("-74.042001");
    }

    @Test
    void aceptaLosLimites() {
        Coordenada c = new Coordenada(new BigDecimal("90"), new BigDecimal("-180"));

        assertThat(c.latitud()).isEqualByComparingTo("90");
        assertThat(c.longitud()).isEqualByComparingTo("-180");
    }

    @Test
    void latitudFueraDeRango_lanzaExcepcion() {
        assertThatThrownBy(() -> Coordenada.de(90.5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitud");
    }

    @Test
    void longitudFueraDeRango_lanzaExcepcion() {
        assertThatThrownBy(() -> Coordenada.de(0, -180.5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitud");
    }

    @Test
    void conValoresNulos_lanzaExcepcion() {
        assertThatThrownBy(() -> new Coordenada(null, BigDecimal.ZERO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Coordenada(BigDecimal.ZERO, null)).isInstanceOf(NullPointerException.class);
    }
}
