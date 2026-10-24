package edu.eci.dosw.campusseguro.campus.gateway;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Espacio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspacioGatewayImplTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID OTRO_PISO = uuid(3);
    private static final UUID ESPACIO = uuid(10);

    @Mock
    private EspacioRepository espacioRepository;

    @Mock
    private PisoRepository pisoRepository;

    @Mock
    private EdificioRepository edificioRepository;

    @InjectMocks
    private EspacioGatewayImpl gateway;

    private static Edificio edificio(Estado estado) {
        Edificio e = new Edificio();
        e.setId(EDIFICIO);
        e.setEstado(estado);
        return e;
    }

    private static Piso piso(UUID id, Estado estado) {
        Piso p = new Piso();
        p.setId(id);
        p.setEdificioId(EDIFICIO);
        p.setEstado(estado);
        return p;
    }

    private static Espacio espacio(UUID id, UUID pisoId, Estado estado) {
        Espacio e = new Espacio();
        e.setId(id);
        e.setEdificioId(EDIFICIO);
        e.setPisoId(pisoId);
        e.setNombre("Salón " + id);
        e.setTipo(TipoEspacio.SALON);
        e.setEstado(estado);
        return e;
    }

    private void dado(Estado espacio, Estado piso, Estado edificio) {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.of(espacio(ESPACIO, PISO, espacio)));
        lenient().when(pisoRepository.findById(PISO)).thenReturn(Optional.of(piso(PISO, piso)));
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(edificio(edificio)));
    }

    @Test
    void buscarPorId_espacioPisoYEdificioActivos_estaDisponible() {
        dado(Estado.ACTIVO, Estado.ACTIVO, Estado.ACTIVO);

        assertThat(gateway.buscarPorId(ESPACIO)).get().extracting(EspacioInfo::activo).isEqualTo(true);
    }

    @Test
    void buscarPorId_espacioInactivo_noEstaDisponible() {
        dado(Estado.INACTIVO, Estado.ACTIVO, Estado.ACTIVO);

        assertThat(gateway.buscarPorId(ESPACIO)).get().extracting(EspacioInfo::activo).isEqualTo(false);
    }

    @Test
    void buscarPorId_pisoInactivo_noEstaDisponible() {
        dado(Estado.ACTIVO, Estado.INACTIVO, Estado.ACTIVO);

        assertThat(gateway.buscarPorId(ESPACIO)).get().extracting(EspacioInfo::activo).isEqualTo(false);
    }

    @Test
    void buscarPorId_edificioInactivo_noEstaDisponible() {
        dado(Estado.ACTIVO, Estado.ACTIVO, Estado.INACTIVO);

        assertThat(gateway.buscarPorId(ESPACIO)).get().extracting(EspacioInfo::activo).isEqualTo(false);
    }

    @Test
    void buscarPorId_espacioInexistente_vacio() {
        when(espacioRepository.findById(ESPACIO)).thenReturn(Optional.empty());

        assertThat(gateway.buscarPorId(ESPACIO)).isEmpty();
    }

    @Test
    void listarPorEdificio_marcaDisponiblesSegunSuPiso() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(edificio(Estado.ACTIVO)));
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO))
                .thenReturn(List.of(piso(PISO, Estado.ACTIVO), piso(OTRO_PISO, Estado.INACTIVO)));
        when(espacioRepository.findByEdificioId(EDIFICIO)).thenReturn(List.of(
                espacio(uuid(10), PISO, Estado.ACTIVO),
                espacio(uuid(11), PISO, Estado.INACTIVO),
                espacio(uuid(12), OTRO_PISO, Estado.ACTIVO)));

        List<EspacioInfo> espacios = gateway.listarPorEdificio(EDIFICIO);

        assertThat(espacios).extracting(EspacioInfo::id, EspacioInfo::activo).containsExactly(
                tuple(uuid(10), true),
                tuple(uuid(11), false),
                tuple(uuid(12), false));
    }

    @Test
    void listarPorEdificio_edificioInactivo_ningunoDisponible() {
        when(edificioRepository.findById(EDIFICIO)).thenReturn(Optional.of(edificio(Estado.INACTIVO)));
        when(pisoRepository.findByEdificioIdOrderByNumeroAsc(EDIFICIO)).thenReturn(List.of(piso(PISO, Estado.ACTIVO)));
        when(espacioRepository.findByEdificioId(EDIFICIO))
                .thenReturn(List.of(espacio(uuid(10), PISO, Estado.ACTIVO)));

        assertThat(gateway.listarPorEdificio(EDIFICIO)).extracting(EspacioInfo::activo).containsExactly(false);
    }
}
