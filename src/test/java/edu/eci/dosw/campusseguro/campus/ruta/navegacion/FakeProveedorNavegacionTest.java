package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaCalculada;
import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import org.junit.jupiter.api.Test;

class FakeProveedorNavegacionTest {

    private static final Coordenada ORIGEN = Coordenada.de(4.782, -74.042);
    private static final Coordenada DESTINO = Coordenada.de(4.784, -74.040);

    @Test
    void calcula_unaLineaRectaEntreLosDosPuntos() {
        FakeProveedorNavegacion fake = new FakeProveedorNavegacion();

        RutaCalculada ruta = fake.calcularRutaPeatonal(ORIGEN, DESTINO);

        assertThat(ruta.geometria()).containsExactly(ORIGEN, DESTINO);
        assertThat(ruta.distanciaMetros()).isBetween(300.0, 350.0);
        assertThat(ruta.duracionSegundos()).isEqualTo(ruta.distanciaMetros() / 1.4);
        assertThat(ruta.pasos()).hasSize(2);
        assertThat(fake.llamadas()).isEqualTo(1);
    }

    @Test
    void conMismoOrigenYDestino_ladistanciaEsCero() {
        assertThat(new FakeProveedorNavegacion().calcularRutaPeatonal(ORIGEN, ORIGEN).distanciaMetros()).isZero();
    }

    @Test
    void fallandoCon_lanzaLaExcepcionIndicada() {
        FakeProveedorNavegacion fake = FakeProveedorNavegacion.fallandoCon(new NavegacionNoDisponibleException("caído"));

        assertThatThrownBy(() -> fake.calcularRutaPeatonal(ORIGEN, DESTINO))
                .isInstanceOf(NavegacionNoDisponibleException.class);
        assertThat(fake.llamadas()).isEqualTo(1);
    }
}
