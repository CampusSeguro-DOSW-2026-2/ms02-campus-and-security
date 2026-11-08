package edu.eci.dosw.campusseguro.campus.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ModificarSalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaDuplicadaException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalidaServiceTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID OTRO_PISO = uuid(3);
    private static final UUID SALIDA = uuid(20);
    private static final BigDecimal LATITUD = new BigDecimal("4.782710");
    private static final BigDecimal LONGITUD = new BigDecimal("-74.042630");

    @Mock
    private SalidaRepository salidaRepository;
    @Mock
    private PisoRepository pisoRepository;
    @Mock
    private EdificioRepository edificioRepository;

    @InjectMocks
    private SalidaService salidaService;

    private void edificio(Estado estado) {
        Edificio e = new Edificio();
        e.setId(EDIFICIO);
        e.setNombre("Bloque G");
        e.setEstado(estado);
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(e));
    }

    private static Piso piso(UUID id, UUID edificioId, int numero, Estado estado) {
        Piso p = new Piso();
        p.setId(id);
        p.setEdificioId(edificioId);
        p.setNumero(numero);
        p.setEstado(estado);
        return p;
    }

    private void piso(Estado estado) {
        when(pisoRepository.findById(PISO)).thenReturn(Optional.of(piso(PISO, EDIFICIO, 1, estado)));
    }

    private Salida salida(Estado estado) {
        Salida s = new Salida();
        s.setId(SALIDA);
        s.setEdificioId(EDIFICIO);
        s.setPisoId(PISO);
        s.setCodigo("G-S1");
        s.setNombre("Salida principal");
        s.setLatitud(LATITUD);
        s.setLongitud(LONGITUD);
        s.setEstado(estado);
        return s;
    }

    private void existe(Salida s) {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.of(s));
    }

    private void guardarDevuelveLoMismo() {
        when(salidaRepository.save(any(Salida.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static SalidaRequest request(String codigo) {
        return new SalidaRequest(PISO, codigo, "  Salida principal  ", LATITUD, LONGITUD);
    }

    @Test
    void crear_guardaLaSalidaConSusCoordenadas() {
        edificio(Estado.ACTIVO);
        piso(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        SalidaResponse respuesta = salidaService.crear(EDIFICIO, request("  G-S1  "));

        assertThat(respuesta.codigo()).isEqualTo("G-S1");
        assertThat(respuesta.nombre()).isEqualTo("Salida principal");
        assertThat(respuesta.latitud()).isEqualTo(LATITUD);
        assertThat(respuesta.longitud()).isEqualTo(LONGITUD);
        assertThat(respuesta.pisoNumero()).isEqualTo(1);
        assertThat(respuesta.edificioNombre()).isEqualTo("Bloque G");
        assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
        assertThat(respuesta.disponible()).isTrue();
    }

    @Test
    void crear_enEdificioInexistente_falla() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salidaService.crear(EDIFICIO, request("G-S1")))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void crear_enEdificioInactivo_seRechaza() {
        edificio(Estado.INACTIVO);

        assertThatThrownBy(() -> salidaService.crear(EDIFICIO, request("G-S1")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("edificio inactivo");
        verify(salidaRepository, never()).save(any());
    }

    @Test
    void crear_conPisoInexistente_falla() {
        edificio(Estado.ACTIVO);
        when(pisoRepository.findById(PISO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salidaService.crear(EDIFICIO, request("G-S1")))
                .isInstanceOf(PisoNoEncontradoException.class);
    }

    @Test
    void crear_conPisoDeOtroEdificio_seRechaza() {
        edificio(Estado.ACTIVO);
        when(pisoRepository.findById(PISO)).thenReturn(Optional.of(piso(PISO, uuid(9), 1, Estado.ACTIVO)));

        assertThatThrownBy(() -> salidaService.crear(EDIFICIO, request("G-S1")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece al edificio");
        verify(salidaRepository, never()).save(any());
    }

    @Test
    void crear_enPisoInactivo_seRechaza() {
        edificio(Estado.ACTIVO);
        piso(Estado.INACTIVO);

        assertThatThrownBy(() -> salidaService.crear(EDIFICIO, request("G-S1")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("piso inactivo");
    }

    @Test
    void crear_conCodigoRepetidoEnElEdificio_seRechaza() {
        edificio(Estado.ACTIVO);
        piso(Estado.ACTIVO);
        when(salidaRepository.existsByEdificioIdAndCodigo(EDIFICIO, "G-S1")).thenReturn(true);

        assertThatThrownBy(() -> salidaService.crear(EDIFICIO, request("G-S1")))
                .isInstanceOf(SalidaDuplicadaException.class);
        verify(salidaRepository, never()).save(any());
    }

    @Test
    void modificar_cambiaNombreYCoordenadasSinTocarElCodigo() {
        existe(salida(Estado.ACTIVO));
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();
        BigDecimal nuevaLatitud = new BigDecimal("4.783000");

        SalidaResponse respuesta = salidaService.modificar(SALIDA,
                new ModificarSalidaRequest(PISO, " Salida norte ", nuevaLatitud, LONGITUD));

        assertThat(respuesta.nombre()).isEqualTo("Salida norte");
        assertThat(respuesta.latitud()).isEqualTo(nuevaLatitud);
        assertThat(respuesta.codigo()).isEqualTo("G-S1");
    }

    @Test
    void modificar_puedeMoverlaAOtroPisoActivoDelMismoEdificio() {
        existe(salida(Estado.ACTIVO));
        when(pisoRepository.findById(OTRO_PISO)).thenReturn(Optional.of(piso(OTRO_PISO, EDIFICIO, -1, Estado.ACTIVO)));
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        SalidaResponse respuesta = salidaService.modificar(SALIDA,
                new ModificarSalidaRequest(OTRO_PISO, "Salida del sótano", LATITUD, LONGITUD));

        assertThat(respuesta.pisoId()).isEqualTo(OTRO_PISO);
        assertThat(respuesta.pisoNumero()).isEqualTo(-1);
    }

    @Test
    void modificar_aUnPisoDeOtroEdificio_seRechaza() {
        existe(salida(Estado.ACTIVO));
        when(pisoRepository.findById(OTRO_PISO)).thenReturn(Optional.of(piso(OTRO_PISO, uuid(9), 1, Estado.ACTIVO)));

        assertThatThrownBy(() -> salidaService.modificar(SALIDA,
                new ModificarSalidaRequest(OTRO_PISO, "Salida", LATITUD, LONGITUD)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece al edificio");
        verify(salidaRepository, never()).save(any());
    }

    @Test
    void modificar_salidaInexistente_falla() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salidaService.modificar(SALIDA,
                new ModificarSalidaRequest(PISO, "Salida", LATITUD, LONGITUD)))
                .isInstanceOf(SalidaNoEncontradaException.class);
    }

    @Test
    void inactivar_laDejaInactivaYNoDisponible() {
        existe(salida(Estado.ACTIVO));
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        SalidaResponse respuesta = salidaService.inactivar(SALIDA);

        assertThat(respuesta.estado()).isEqualTo(Estado.INACTIVO);
        assertThat(respuesta.disponible()).isFalse();
    }

    @Test
    void inactivar_salidaYaInactiva_seRechaza() {
        existe(salida(Estado.INACTIVO));

        assertThatThrownBy(() -> salidaService.inactivar(SALIDA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está inactiva");
    }

    @Test
    void reactivar_conPisoYEdificioActivos_laDejaActiva() {
        existe(salida(Estado.INACTIVO));
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        assertThat(salidaService.reactivar(SALIDA).estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void reactivar_salidaYaActiva_seRechaza() {
        existe(salida(Estado.ACTIVO));

        assertThatThrownBy(() -> salidaService.reactivar(SALIDA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está activa");
    }

    @Test
    void reactivar_conPisoInactivo_seRechaza() {
        existe(salida(Estado.INACTIVO));
        piso(Estado.INACTIVO);
        edificio(Estado.ACTIVO);

        assertThatThrownBy(() -> salidaService.reactivar(SALIDA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("debe reactivarse primero");
        verify(salidaRepository, never()).save(any());
    }

    @Test
    void reactivar_conEdificioInactivo_seRechaza() {
        existe(salida(Estado.INACTIVO));
        piso(Estado.ACTIVO);
        edificio(Estado.INACTIVO);

        assertThatThrownBy(() -> salidaService.reactivar(SALIDA))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void listarPorEdificio_porDefectoSoloLasDisponibles() {
        edificio(Estado.ACTIVO);
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO))
                .thenReturn(List.of(piso(PISO, EDIFICIO, 1, Estado.ACTIVO)));
        Salida inactiva = salida(Estado.INACTIVO);
        inactiva.setId(uuid(21));
        when(salidaRepository.findByEdificioIdOrderByCodigoAsc(EDIFICIO))
                .thenReturn(List.of(salida(Estado.ACTIVO), inactiva));

        assertThat(salidaService.listarPorEdificio(EDIFICIO, false)).extracting(SalidaResponse::id)
                .containsExactly(SALIDA);
        assertThat(salidaService.listarPorEdificio(EDIFICIO, true)).extracting(SalidaResponse::disponible)
                .containsExactly(true, false);
    }

    @Test
    void listarPorEdificio_conPisoInactivo_laMarcaNoDisponible() {
        edificio(Estado.ACTIVO);
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO))
                .thenReturn(List.of(piso(PISO, EDIFICIO, 1, Estado.INACTIVO)));
        when(salidaRepository.findByEdificioIdOrderByCodigoAsc(EDIFICIO)).thenReturn(List.of(salida(Estado.ACTIVO)));

        assertThat(salidaService.listarPorEdificio(EDIFICIO, false)).isEmpty();
        assertThat(salidaService.listarPorEdificio(EDIFICIO, true)).extracting(SalidaResponse::disponible)
                .containsExactly(false);
    }

    @Test
    void listarPorEdificio_edificioInexistente_falla() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salidaService.listarPorEdificio(EDIFICIO, false))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void obtener_devuelveLaSalida() {
        existe(salida(Estado.ACTIVO));
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);

        SalidaResponse respuesta = salidaService.obtener(SALIDA);

        assertThat(respuesta.id()).isEqualTo(SALIDA);
        assertThat(respuesta.disponible()).isTrue();
    }

    @Test
    void obtener_salidaInexistente_falla() {
        when(salidaRepository.findById(SALIDA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salidaService.obtener(SALIDA))
                .isInstanceOf(SalidaNoEncontradaException.class);
    }
}
