package edu.eci.dosw.campusseguro.campus.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEspacioRequest;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspacioServiceTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID ESPACIO = uuid(10);

    @Mock
    private EspacioRepository espacioRepository;
    @Mock
    private PisoRepository pisoRepository;
    @Mock
    private EdificioRepository edificioRepository;
    @Mock
    private VerificadorRutas verificadorRutas;

    @InjectMocks
    private EspacioService espacioService;

    private Espacio espacioActivo() {
        Espacio e = new Espacio();
        e.setId(ESPACIO);
        e.setEdificioId(EDIFICIO);
        e.setPisoId(PISO);
        e.setCodigo("G-101");
        e.setNombre("Salón 101");
        e.setTipo(TipoEspacio.SALON);
        return e;
    }

    private void piso(Estado estado) {
        Piso p = new Piso();
        p.setId(PISO);
        p.setEdificioId(EDIFICIO);
        p.setNumero(1);
        p.setEstado(estado);
        when(pisoRepository.findById(PISO)).thenReturn(Optional.of(p));
    }

    private void edificio(Estado estado) {
        Edificio e = new Edificio();
        e.setId(EDIFICIO);
        e.setEstado(estado);
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(e));
    }

    private void guardarDevuelveLoMismo() {
        when(espacioRepository.save(any(Espacio.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static EspacioRequest request(String codigo) {
        return new EspacioRequest(codigo, "  Salón 101  ", TipoEspacio.SALON);
    }

    @Test
    void crear_loAsociaAlPisoYAlEdificioDelPiso() {
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        EspacioResponse respuesta = espacioService.crear(PISO, request("  G-101  "));

        assertThat(respuesta.pisoId()).isEqualTo(PISO);
        assertThat(respuesta.edificioId()).isEqualTo(EDIFICIO);
        assertThat(respuesta.codigo()).isEqualTo("G-101");
        assertThat(respuesta.nombre()).isEqualTo("Salón 101");
        assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void crear_aceptaEspacioComun() {
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        EspacioResponse respuesta = espacioService.crear(PISO,
                new EspacioRequest("G-CAF", "Cafetería", TipoEspacio.ESPACIO_COMUN));

        assertThat(respuesta.tipo()).isEqualTo(TipoEspacio.ESPACIO_COMUN);
    }

    @Test
    void crear_enPisoInexistente_falla() {
        when(pisoRepository.findById(PISO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> espacioService.crear(PISO, request("G-101")))
                .isInstanceOf(PisoNoEncontradoException.class);
    }

    @Test
    void crear_enEdificioInactivo_seRechaza() {
        piso(Estado.ACTIVO);
        edificio(Estado.INACTIVO);

        assertThatThrownBy(() -> espacioService.crear(PISO, request("G-101")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("edificio inactivo");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void crear_enPisoInactivo_seRechaza() {
        piso(Estado.INACTIVO);
        edificio(Estado.ACTIVO);

        assertThatThrownBy(() -> espacioService.crear(PISO, request("G-101")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("piso inactivo");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void crear_conCodigoRepetidoEnElEdificio_seRechaza() {
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        when(espacioRepository.existsByEdificioIdAndCodigo(EDIFICIO, "G-101")).thenReturn(true);

        assertThatThrownBy(() -> espacioService.crear(PISO, request("G-101")))
                .isInstanceOf(EspacioDuplicadoException.class);
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void modificar_cambiaNombreYTipoSinTocarElCodigo() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));
        guardarDevuelveLoMismo();

        EspacioResponse respuesta = espacioService.modificar(ESPACIO,
                new ModificarEspacioRequest("  Laboratorio 101 ", TipoEspacio.LABORATORIO));

        assertThat(respuesta.nombre()).isEqualTo("Laboratorio 101");
        assertThat(respuesta.tipo()).isEqualTo(TipoEspacio.LABORATORIO);
        assertThat(respuesta.codigo()).isEqualTo("G-101");
    }

    @Test
    void modificar_espacioInexistente_falla() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> espacioService.modificar(ESPACIO,
                new ModificarEspacioRequest("Salón", TipoEspacio.SALON)))
                .isInstanceOf(EspacioNoEncontradoException.class);
    }

    @Test
    void listarPorPiso_devuelveSusEspacios() {
        piso(Estado.ACTIVO);
        when(espacioRepository.findByPisoId(PISO)).thenReturn(List.of(espacioActivo()));

        assertThat(espacioService.listarPorPiso(PISO)).extracting(EspacioResponse::id).containsExactly(ESPACIO);
    }

    @Test
    void listarPorEdificio_edificioInexistente_falla() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> espacioService.listarPorEdificio(EDIFICIO))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void listarPorEdificio_devuelveSusEspacios() {
        edificio(Estado.ACTIVO);
        when(espacioRepository.findByEdificioId(EDIFICIO)).thenReturn(List.of(espacioActivo()));

        assertThat(espacioService.listarPorEdificio(EDIFICIO)).extracting(EspacioResponse::codigo)
                .containsExactly("G-101");
    }

    @Test
    void obtener_devuelveElEspacio() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));

        assertThat(espacioService.obtener(ESPACIO).nombre()).isEqualTo("Salón 101");
    }

    @Test
    void inactivar_sinRutasActivas_loDejaInactivo() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));
        when(verificadorRutas.rutasActivasDeEspacio(ESPACIO)).thenReturn(List.of());
        guardarDevuelveLoMismo();

        EspacioResponse respuesta = espacioService.inactivar(ESPACIO);

        assertThat(respuesta.estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void inactivar_conRutaActiva_seRechazaYNoGuarda() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));
        when(verificadorRutas.rutasActivasDeEspacio(ESPACIO)).thenReturn(List.of(uuid(20)));

        assertThatThrownBy(() -> espacioService.inactivar(ESPACIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining(uuid(20).toString());
        verify(espacioRepository, never()).save(any());
    }

    private Espacio espacioInactivo() {
        Espacio e = espacioActivo();
        e.setEstado(Estado.INACTIVO);
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(e));
        return e;
    }

    @Test
    void reactivar_conPisoYEdificioActivos_loDejaActivo() {
        espacioInactivo();
        piso(Estado.ACTIVO);
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        assertThat(espacioService.reactivar(ESPACIO).estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void reactivar_espacioYaActivo_seRechaza() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacioActivo()));

        assertThatThrownBy(() -> espacioService.reactivar(ESPACIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está activo");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void reactivar_conPisoInactivo_seRechaza() {
        espacioInactivo();
        piso(Estado.INACTIVO);
        edificio(Estado.ACTIVO);

        assertThatThrownBy(() -> espacioService.reactivar(ESPACIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("debe reactivarse primero");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void reactivar_conEdificioInactivo_seRechaza() {
        espacioInactivo();
        piso(Estado.ACTIVO);
        edificio(Estado.INACTIVO);

        assertThatThrownBy(() -> espacioService.reactivar(ESPACIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("debe reactivarse primero");
        verify(espacioRepository, never()).save(any());
    }
}
