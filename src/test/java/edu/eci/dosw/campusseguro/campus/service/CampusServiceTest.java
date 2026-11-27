package edu.eci.dosw.campusseguro.campus.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.CampusRequest;
import edu.eci.dosw.campusseguro.campus.dto.CampusResponse;
import edu.eci.dosw.campusseguro.campus.entity.Campus;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.repository.CampusRepository;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaAbiertaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private EdificioRepository edificioRepository;

    @Mock
    private ZonaAbiertaRepository zonaAbiertaRepository;

    @InjectMocks
    private CampusService campusService;

    private Campus campus(UUID id, String nombre, Estado estado) {
        Campus c = new Campus();
        c.setId(id);
        c.setNombre(nombre);
        c.setEstado(estado);
        return c;
    }

    @Test
    void crear_guardaCampusActivoConIdGeneradoYLimpiaEspacios() {
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> {
            Campus c = inv.getArgument(0);
            c.setId(uuid(1));
            return c;
        });

        CampusResponse respuesta = campusService.crear(new CampusRequest(" Campus norte ", "Desc"));

        assertThat(respuesta.id()).isEqualTo(uuid(1));
        assertThat(respuesta.nombre()).isEqualTo("Campus norte");
        assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void modificar_cambiaNombreYDescripcion() {
        when(campusRepository.findById(uuid(1))).thenReturn(Optional.of(campus(uuid(1), "Viejo", Estado.ACTIVO)));
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));

        CampusResponse respuesta = campusService.modificar(uuid(1), new CampusRequest(" Nuevo ", "Otra"));

        assertThat(respuesta.id()).isEqualTo(uuid(1));
        assertThat(respuesta.nombre()).isEqualTo("Nuevo");
        assertThat(respuesta.descripcion()).isEqualTo("Otra");
    }

    @Test
    void modificar_noExiste_lanzaExcepcion() {
        when(campusRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.modificar(uuid(99), new CampusRequest("N", null)))
                .isInstanceOf(CampusNoEncontradoException.class);
    }

    @Test
    void inactivar_sinHijosActivos_loDejaInactivo() {
        when(campusRepository.findById(uuid(1))).thenReturn(Optional.of(campus(uuid(1), "Campus 1", Estado.ACTIVO)));
        when(edificioRepository.existsByCampusIdAndEstado(uuid(1), Estado.ACTIVO)).thenReturn(false);
        when(zonaAbiertaRepository.existsByCampusIdAndEstado(uuid(1), Estado.ACTIVO)).thenReturn(false);
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(campusService.inactivar(uuid(1)).estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void inactivar_conEdificiosActivos_lanzaReglaDeNegocioYNoGuarda() {
        when(campusRepository.findById(uuid(1))).thenReturn(Optional.of(campus(uuid(1), "Campus 1", Estado.ACTIVO)));
        when(edificioRepository.existsByCampusIdAndEstado(uuid(1), Estado.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> campusService.inactivar(uuid(1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("edificios activos");

        verify(campusRepository, never()).save(any());
    }

    @Test
    void inactivar_conZonasAbiertasActivas_lanzaReglaDeNegocioYNoGuarda() {
        when(campusRepository.findById(uuid(1))).thenReturn(Optional.of(campus(uuid(1), "Campus 1", Estado.ACTIVO)));
        when(edificioRepository.existsByCampusIdAndEstado(uuid(1), Estado.ACTIVO)).thenReturn(false);
        when(zonaAbiertaRepository.existsByCampusIdAndEstado(uuid(1), Estado.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> campusService.inactivar(uuid(1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("zonas abiertas activas");

        verify(campusRepository, never()).save(any());
    }

    @Test
    void reactivar_campusInactivo_loDejaActivo() {
        when(campusRepository.findById(uuid(1))).thenReturn(Optional.of(campus(uuid(1), "Campus 1", Estado.INACTIVO)));
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(campusService.reactivar(uuid(1)).estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void reactivar_campusYaActivo_lanzaReglaDeNegocio() {
        when(campusRepository.findById(uuid(1))).thenReturn(Optional.of(campus(uuid(1), "Campus 1", Estado.ACTIVO)));

        assertThatThrownBy(() -> campusService.reactivar(uuid(1)))
                .isInstanceOf(ReglaNegocioException.class);

        verify(campusRepository, never()).save(any());
    }

    @Test
    void listar_porDefectoDevuelveSoloLosActivos() {
        when(campusRepository.findAll()).thenReturn(List.of(
                campus(uuid(1), "Campus 1", Estado.ACTIVO), campus(uuid(2), "Campus 2", Estado.INACTIVO)));

        assertThat(campusService.listar(false)).extracting(CampusResponse::nombre).containsExactly("Campus 1");
    }

    @Test
    void listar_conIncluirInactivos_devuelveTodos() {
        when(campusRepository.findAll()).thenReturn(List.of(
                campus(uuid(1), "Campus 1", Estado.ACTIVO), campus(uuid(2), "Campus 2", Estado.INACTIVO)));

        assertThat(campusService.listar(true)).hasSize(2);
    }

    @Test
    void obtener_noExiste_lanzaExcepcion() {
        when(campusRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.obtener(uuid(99)))
                .isInstanceOf(CampusNoEncontradoException.class);
    }
}
