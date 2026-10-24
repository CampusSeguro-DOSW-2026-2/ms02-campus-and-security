package edu.eci.dosw.campusseguro.campus.service;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.PisoRequest;
import edu.eci.dosw.campusseguro.campus.dto.PisoResponse;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
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
class PisoServiceTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);

    @Mock
    private PisoRepository pisoRepository;
    @Mock
    private EdificioRepository edificioRepository;
    @Mock
    private EspacioRepository espacioRepository;

    @InjectMocks
    private PisoService pisoService;

    private void edificio(Estado estado) {
        Edificio e = new Edificio();
        e.setId(EDIFICIO);
        e.setEstado(estado);
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(e));
    }

    private Piso piso() {
        Piso p = new Piso();
        p.setId(PISO);
        p.setEdificioId(EDIFICIO);
        p.setNumero(1);
        p.setNombre("Primer piso");
        when(pisoRepository.findById(PISO)).thenReturn(Optional.of(p));
        return p;
    }

    private void guardarDevuelveLoMismo() {
        when(pisoRepository.save(any(Piso.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crear_guardaElPisoActivoEnElEdificio() {
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        PisoResponse respuesta = pisoService.crear(EDIFICIO, new PisoRequest(-1, "  Sótano 1  "));

        assertThat(respuesta.edificioId()).isEqualTo(EDIFICIO);
        assertThat(respuesta.numero()).isEqualTo(-1);
        assertThat(respuesta.nombre()).isEqualTo("Sótano 1");
        assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void crear_conNombreEnBlanco_loGuardaSinNombre() {
        edificio(Estado.ACTIVO);
        guardarDevuelveLoMismo();

        PisoResponse respuesta = pisoService.crear(EDIFICIO, new PisoRequest(2, "   "));

        assertThat(respuesta.nombre()).isNull();
    }

    @Test
    void crear_enEdificioInexistente_falla() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pisoService.crear(EDIFICIO, new PisoRequest(1, null)))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void crear_enEdificioInactivo_seRechaza() {
        edificio(Estado.INACTIVO);

        assertThatThrownBy(() -> pisoService.crear(EDIFICIO, new PisoRequest(1, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");
        verify(pisoRepository, never()).save(any());
    }

    @Test
    void crear_conNumeroRepetido_seRechaza() {
        edificio(Estado.ACTIVO);
        when(pisoRepository.existsByEdificioIdAndNumero(EDIFICIO, 1)).thenReturn(true);

        assertThatThrownBy(() -> pisoService.crear(EDIFICIO, new PisoRequest(1, null)))
                .isInstanceOf(PisoDuplicadoException.class);
        verify(pisoRepository, never()).save(any());
    }

    @Test
    void modificar_cambiaNumeroYNombre() {
        piso();
        guardarDevuelveLoMismo();

        PisoResponse respuesta = pisoService.modificar(PISO, new PisoRequest(3, "Tercer piso"));

        assertThat(respuesta.numero()).isEqualTo(3);
        assertThat(respuesta.nombre()).isEqualTo("Tercer piso");
    }

    @Test
    void modificar_conNumeroDeOtroPiso_seRechaza() {
        piso();
        when(pisoRepository.existsByEdificioIdAndNumeroAndIdNot(EDIFICIO, 3, PISO)).thenReturn(true);

        assertThatThrownBy(() -> pisoService.modificar(PISO, new PisoRequest(3, null)))
                .isInstanceOf(PisoDuplicadoException.class);
        verify(pisoRepository, never()).save(any());
    }

    @Test
    void modificar_pisoInexistente_falla() {
        when(pisoRepository.findById(PISO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pisoService.modificar(PISO, new PisoRequest(1, null)))
                .isInstanceOf(PisoNoEncontradoException.class);
    }

    @Test
    void inactivar_sinEspaciosActivos_loDejaInactivo() {
        piso();
        guardarDevuelveLoMismo();

        assertThat(pisoService.inactivar(PISO).estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void inactivar_conEspaciosActivos_seRechaza() {
        piso();
        when(espacioRepository.existsByPisoIdAndEstado(PISO, Estado.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> pisoService.inactivar(PISO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("espacios activos");
        verify(pisoRepository, never()).save(any());
    }

    @Test
    void listarPorEdificio_devuelveSusPisos() {
        edificio(Estado.INACTIVO);
        Piso p = new Piso();
        p.setId(PISO);
        p.setEdificioId(EDIFICIO);
        p.setNumero(1);
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO)).thenReturn(List.of(p));

        assertThat(pisoService.listarPorEdificio(EDIFICIO)).extracting(PisoResponse::id).containsExactly(PISO);
    }

    @Test
    void listarPorEdificio_edificioInexistente_falla() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pisoService.listarPorEdificio(EDIFICIO))
                .isInstanceOf(EdificioNoEncontradoException.class);
    }

    @Test
    void obtener_devuelveElPiso() {
        piso();

        assertThat(pisoService.obtener(PISO).numero()).isEqualTo(1);
    }
}
