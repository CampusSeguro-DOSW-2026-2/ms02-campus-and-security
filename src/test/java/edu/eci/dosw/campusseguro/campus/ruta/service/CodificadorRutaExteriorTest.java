package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.List;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.PasoRuta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodificadorRutaExteriorTest {

    @Test
    void geometria_ida_y_vuelta() {
        List<Coordenada> original = List.of(Coordenada.de(4.78271, -74.04263), Coordenada.de(4.7835, -74.0431));

        String texto = CodificadorRutaExterior.codificarGeometria(original);

        assertThat(CodificadorRutaExterior.decodificarGeometria(texto)).isEqualTo(original);
    }

    @Test
    void pasos_ida_y_vuelta_conservaLaInstruccionEnUnaLinea() {
        List<PasoRuta> pasos = List.of(
                new PasoRuta("Gira a la izquierda | Calle 1\nluego recto", 40.5, 30),
                new PasoRuta("Has llegado", 0, 0));

        List<PasoRuta> resultado = CodificadorRutaExterior.decodificarPasos(CodificadorRutaExterior.codificarPasos(pasos));

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).instruccion()).isEqualTo("Gira a la izquierda | Calle 1 luego recto");
        assertThat(resultado.get(0).distanciaMetros()).isEqualTo(40.5);
    }

    @Test
    void textoVacio_devuelveListaVacia() {
        assertThat(CodificadorRutaExterior.decodificarGeometria(null)).isEmpty();
        assertThat(CodificadorRutaExterior.decodificarPasos("")).isEmpty();
    }
}
