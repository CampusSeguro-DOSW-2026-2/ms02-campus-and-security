package edu.eci.dosw.campusseguro.campus.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalidaGatewayImplTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID SALIDA = uuid(20);

    @Mock
    private SalidaRepository salidaRepository;
    @Mock
    private PisoRepository pisoRepository;
    @Mock
    private EdificioRepository edificioRepository;

    @InjectMocks
    private SalidaGatewayImpl gateway;

    private static Salida salida(Estado estado) {
        Salida s = new Salida();
        s.setId(SALIDA);
        s.setEdificioId(EDIFICIO);
        s.setPisoId(PISO);
        s.setNombre("Salida principal");
        s.setLatitud(new BigDecimal("4.782710"));
        s.setLongitud(new BigDecimal("-74.042630"));
        s.setEstado(estado);
        return s;
    }

    private static Piso piso(Estado estado) {
        Piso p = new Piso();
        p.setId(PISO);
        p.setEdificioId(EDIFICIO);
        p.setEstado(estado);
        return p;
    }

    private void contenedores(Estado edificio, Estado piso) {
        Edificio e = new Edificio();
        e.setId(EDIFICIO);
        e.setEstado(edificio);
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(e));
        lenient().when(pisoRepository.findById(PISO)).thenReturn(Optional.of(piso(piso)));
    }

    @Test
    void buscarPorId_salidaActivaConContenedoresActivos_estaDisponibleConSusCoordenadas() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(salida(Estado.ACTIVO)));
        contenedores(Estado.ACTIVO, Estado.ACTIVO);

        SalidaInfo info = gateway.buscarPorId(SALIDA).orElseThrow();

        assertThat(info.disponible()).isTrue();
        assertThat(info.latitud()).isEqualByComparingTo("4.78271");
        assertThat(info.pisoId()).isEqualTo(PISO);
    }

    @Test
    void buscarPorId_salidaInactiva_noEstaDisponible() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(salida(Estado.INACTIVO)));
        contenedores(Estado.ACTIVO, Estado.ACTIVO);

        assertThat(gateway.buscarPorId(SALIDA).orElseThrow().disponible()).isFalse();
    }

    @Test
    void buscarPorId_conPisoInactivo_noEstaDisponible() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(salida(Estado.ACTIVO)));
        contenedores(Estado.ACTIVO, Estado.INACTIVO);

        assertThat(gateway.buscarPorId(SALIDA).orElseThrow().disponible()).isFalse();
    }

    @Test
    void buscarPorId_conEdificioInactivo_noEstaDisponible() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(salida(Estado.ACTIVO)));
        contenedores(Estado.INACTIVO, Estado.ACTIVO);

        assertThat(gateway.buscarPorId(SALIDA).orElseThrow().disponible()).isFalse();
    }

    @Test
    void buscarPorId_salidaInexistente_devuelveVacio() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.empty());

        assertThat(gateway.buscarPorId(SALIDA)).isEmpty();
    }

    @Test
    void listarPorEdificio_marcaDisponibilidadSegunElPiso() {
        contenedores(Estado.ACTIVO, Estado.ACTIVO);
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO)).thenReturn(List.of(piso(Estado.INACTIVO)));
        when(salidaRepository.findByEdificioIdOrderByNombreAsc(EDIFICIO)).thenReturn(List.of(salida(Estado.ACTIVO)));

        assertThat(gateway.listarPorEdificio(EDIFICIO)).extracting(SalidaInfo::disponible).containsExactly(false);
    }

    @Test
    void buscarPorId_sinEdificioNiPisoRegistrados_noEstaDisponible() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(salida(Estado.ACTIVO)));
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        assertThat(gateway.buscarPorId(SALIDA).orElseThrow().disponible()).isFalse();
    }

    @Test
    void listarPorEdificio_conEdificioInactivo_ningunaEstaDisponible() {
        contenedores(Estado.INACTIVO, Estado.ACTIVO);
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO)).thenReturn(List.of(piso(Estado.ACTIVO)));
        when(salidaRepository.findByEdificioIdOrderByNombreAsc(EDIFICIO)).thenReturn(List.of(salida(Estado.ACTIVO)));

        assertThat(gateway.listarPorEdificio(EDIFICIO)).extracting(SalidaInfo::disponible).containsExactly(false);
    }
}
