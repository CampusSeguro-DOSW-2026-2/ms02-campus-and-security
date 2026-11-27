package edu.eci.dosw.campusseguro.campus.ruta.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.eci.dosw.campusseguro.campus.ruta.event.SalidaUbicacionCambiadaEvent;
import edu.eci.dosw.campusseguro.campus.ruta.event.ZonaSeguraUbicacionCambiadaEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RutasAutoListenerTest {

    @Mock
    private GeneradorRutasExterioresService generador;

    @InjectMocks
    private RutasAutoListener listener;

    @Test
    void alCambiarSalida_generaSusRutas() {
        listener.alCambiarSalida(new SalidaUbicacionCambiadaEvent(uuid(1)));

        verify(generador).generarParaSalida(uuid(1));
    }

    @Test
    void alCambiarZona_generaSusRutas() {
        listener.alCambiarZona(new ZonaSeguraUbicacionCambiadaEvent(uuid(2)));

        verify(generador).generarParaZona(uuid(2));
    }

    @Test
    void siLaGeneracionFalla_noPropagaLaExcepcion() {
        when(generador.generarParaSalida(uuid(1))).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> listener.alCambiarSalida(new SalidaUbicacionCambiadaEvent(uuid(1))))
                .doesNotThrowAnyException();
    }
}
