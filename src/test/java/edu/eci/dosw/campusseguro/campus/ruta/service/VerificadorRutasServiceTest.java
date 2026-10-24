package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificadorRutasServiceTest {

    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private EspacioGateway espacioGateway;

    private VerificadorRutasService verificador;

    private final UUID edificioId = uuid(10);
    private final UUID espacioId = uuid(11);
    private final UUID zonaId = uuid(12);
    private final UUID rutaId = uuid(20);

    @BeforeEach
    void setUp() {
        verificador = new VerificadorRutasService(rutaRepository, espacioGateway);
    }

    private RutaEvacuacion ruta() {
        RutaEvacuacion ruta = new RutaEvacuacion(espacioId, zonaId, null, "Salir por la escalera", EstadoRuta.DISPONIBLE);
        ReflectionTestUtils.setField(ruta, "id", rutaId);
        return ruta;
    }

    @Test
    void rutasActivasDeEspacio_devuelveLasRutasDelEspacio() {
        when(rutaRepository.findByEspacioOrigenIdAndEstadoIn(eq(espacioId), anyCollection()))
                .thenReturn(List.of(ruta()));

        assertThat(verificador.rutasActivasDeEspacio(espacioId)).containsExactly(rutaId);
    }

    @Test
    void rutasActivasDeEspacio_soloConsultaDisponiblesYRestringidas() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<EstadoRuta>> estados = ArgumentCaptor.forClass(Collection.class);
        when(rutaRepository.findByEspacioOrigenIdAndEstadoIn(eq(espacioId), estados.capture()))
                .thenReturn(List.of());

        assertThat(verificador.rutasActivasDeEspacio(espacioId)).isEmpty();
        assertThat(estados.getValue()).containsExactlyInAnyOrder(EstadoRuta.DISPONIBLE, EstadoRuta.RESTRINGIDA);
    }

    @Test
    void rutasActivasDeZonaSegura_devuelveLasRutasQueLaTienenDeDestino() {
        when(rutaRepository.findByZonaSeguraDestinoIdAndEstadoIn(eq(zonaId), anyCollection()))
                .thenReturn(List.of(ruta()));

        assertThat(verificador.rutasActivasDeZonaSegura(zonaId)).containsExactly(rutaId);
    }

    @Test
    void rutasActivasDeEdificio_sinEspacios_noConsultaRutas() {
        when(espacioGateway.listarPorEdificio(edificioId)).thenReturn(List.of());

        assertThat(verificador.rutasActivasDeEdificio(edificioId)).isEmpty();
        verify(rutaRepository, never()).findByEspacioOrigenIdInAndEstadoIn(any(), any());
    }

    @Test
    void rutasActivasDeEdificio_devuelveLasRutasDeSusEspacios() {
        when(espacioGateway.listarPorEdificio(edificioId))
                .thenReturn(List.of(new EspacioInfo(espacioId, edificioId, "Salón 101", true)));
        when(rutaRepository.findByEspacioOrigenIdInAndEstadoIn(eq(List.of(espacioId)), anyCollection()))
                .thenReturn(List.of(ruta()));

        assertThat(verificador.rutasActivasDeEdificio(edificioId)).containsExactly(rutaId);
    }

    @Test
    void rutasActivasDeEdificio_incluyeEspaciosNoDisponibles() {
        when(espacioGateway.listarPorEdificio(edificioId))
                .thenReturn(List.of(new EspacioInfo(espacioId, edificioId, "Salón 101", false)));
        when(rutaRepository.findByEspacioOrigenIdInAndEstadoIn(eq(List.of(espacioId)), anyCollection()))
                .thenReturn(List.of());

        assertThat(verificador.rutasActivasDeEdificio(edificioId)).isEmpty();
        verify(rutaRepository).findByEspacioOrigenIdInAndEstadoIn(eq(List.of(espacioId)), anyCollection());
    }
}
