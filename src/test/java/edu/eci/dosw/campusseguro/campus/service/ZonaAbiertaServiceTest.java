package edu.eci.dosw.campusseguro.campus.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Campus;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoZonaAbierta;
import edu.eci.dosw.campusseguro.campus.entity.ZonaAbierta;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaAbiertaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.CampusRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaAbiertaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZonaAbiertaServiceTest {

    private static final UUID CAMPUS = uuid(1);
    private static final UUID ZONA = uuid(2);
    private static final BigDecimal LAT = new BigDecimal("4.782000");
    private static final BigDecimal LNG = new BigDecimal("-74.042000");

    @Mock
    private ZonaAbiertaRepository zonaAbiertaRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private ZonaSeguraRepository zonaSeguraRepository;

    @InjectMocks
    private ZonaAbiertaService zonaAbiertaService;

    private void campus(Estado estado) {
        Campus c = new Campus();
        c.setId(CAMPUS);
        c.setEstado(estado);
        when(campusRepository.findById(CAMPUS)).thenReturn(Optional.of(c));
    }

    private ZonaAbierta zona(Estado estado) {
        ZonaAbierta z = new ZonaAbierta();
        z.setId(ZONA);
        z.setCampusId(CAMPUS);
        z.setNombre("Plazoleta central");
        z.setTipo(TipoZonaAbierta.PLAZOLETA);
        z.setEstado(estado);
        return z;
    }

    @Test
    void crear_guardaLaZonaActivaConSusDatos() {
        campus(Estado.ACTIVO);
        when(zonaAbiertaRepository.save(any(ZonaAbierta.class))).thenAnswer(inv -> {
            ZonaAbierta z = inv.getArgument(0);
            z.setId(ZONA);
            return z;
        });

        ZonaAbiertaResponse respuesta = zonaAbiertaService.crear(CAMPUS,
                new ZonaAbiertaRequest(" Plazoleta ", TipoZonaAbierta.PLAZOLETA, LAT, LNG));

        assertThat(respuesta.id()).isEqualTo(ZONA);
        assertThat(respuesta.campusId()).isEqualTo(CAMPUS);
        assertThat(respuesta.nombre()).isEqualTo("Plazoleta");
        assertThat(respuesta.latitud()).isEqualTo(LAT);
        assertThat(respuesta.estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void crear_sinCoordenadas_esValido() {
        campus(Estado.ACTIVO);
        when(zonaAbiertaRepository.save(any(ZonaAbierta.class))).thenAnswer(inv -> inv.getArgument(0));

        ZonaAbiertaResponse respuesta = zonaAbiertaService.crear(CAMPUS,
                new ZonaAbiertaRequest("Plazoleta", TipoZonaAbierta.PLAZOLETA, null, null));

        assertThat(respuesta.latitud()).isNull();
        assertThat(respuesta.longitud()).isNull();
    }

    @Test
    void crear_soloConLatitud_lanzaReglaDeNegocioYNoGuarda() {
        campus(Estado.ACTIVO);

        assertThatThrownBy(() -> zonaAbiertaService.crear(CAMPUS,
                new ZonaAbiertaRequest("Plazoleta", TipoZonaAbierta.PLAZOLETA, LAT, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("juntas");

        verify(zonaAbiertaRepository, never()).save(any());
    }

    @Test
    void crear_campusInexistente_lanzaExcepcion() {
        when(campusRepository.findById(CAMPUS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zonaAbiertaService.crear(CAMPUS,
                new ZonaAbiertaRequest("Plazoleta", TipoZonaAbierta.PLAZOLETA, null, null)))
                .isInstanceOf(CampusNoEncontradoException.class);
    }

    @Test
    void crear_campusInactivo_lanzaReglaDeNegocioYNoGuarda() {
        campus(Estado.INACTIVO);

        assertThatThrownBy(() -> zonaAbiertaService.crear(CAMPUS,
                new ZonaAbiertaRequest("Plazoleta", TipoZonaAbierta.PLAZOLETA, null, null)))
                .isInstanceOf(ReglaNegocioException.class);

        verify(zonaAbiertaRepository, never()).save(any());
    }

    @Test
    void modificar_cambiaLosCampos() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.ACTIVO)));
        when(zonaAbiertaRepository.save(any(ZonaAbierta.class))).thenAnswer(inv -> inv.getArgument(0));

        ZonaAbiertaResponse respuesta = zonaAbiertaService.modificar(ZONA,
                new ZonaAbiertaRequest(" Cancha ", TipoZonaAbierta.CANCHA, LAT, LNG));

        assertThat(respuesta.id()).isEqualTo(ZONA);
        assertThat(respuesta.nombre()).isEqualTo("Cancha");
        assertThat(respuesta.tipo()).isEqualTo(TipoZonaAbierta.CANCHA);
        assertThat(respuesta.longitud()).isEqualTo(LNG);
    }

    @Test
    void modificar_soloConLongitud_lanzaReglaDeNegocio() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.ACTIVO)));

        assertThatThrownBy(() -> zonaAbiertaService.modificar(ZONA,
                new ZonaAbiertaRequest("Cancha", TipoZonaAbierta.CANCHA, null, LNG)))
                .isInstanceOf(ReglaNegocioException.class);

        verify(zonaAbiertaRepository, never()).save(any());
    }

    @Test
    void modificar_noExiste_lanzaExcepcion() {
        when(zonaAbiertaRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zonaAbiertaService.modificar(uuid(99),
                new ZonaAbiertaRequest("N", TipoZonaAbierta.OTRA, null, null)))
                .isInstanceOf(ZonaAbiertaNoEncontradaException.class);
    }

    @Test
    void inactivar_dejaLaZonaInactiva() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.ACTIVO)));
        when(zonaSeguraRepository.existsByZonaAbiertaIdAndEstado(ZONA, Estado.ACTIVO)).thenReturn(false);
        when(zonaAbiertaRepository.save(any(ZonaAbierta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(zonaAbiertaService.inactivar(ZONA).estado()).isEqualTo(Estado.INACTIVO);
    }

    @Test
    void reactivar_conCampusActivo_dejaLaZonaActiva() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.INACTIVO)));
        campus(Estado.ACTIVO);
        when(zonaAbiertaRepository.save(any(ZonaAbierta.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(zonaAbiertaService.reactivar(ZONA).estado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    void reactivar_conCampusInactivo_lanzaReglaDeNegocioYNoGuarda() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.INACTIVO)));
        campus(Estado.INACTIVO);

        assertThatThrownBy(() -> zonaAbiertaService.reactivar(ZONA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("campus");

        verify(zonaAbiertaRepository, never()).save(any());
    }

    @Test
    void reactivar_zonaYaActiva_lanzaReglaDeNegocio() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.ACTIVO)));

        assertThatThrownBy(() -> zonaAbiertaService.reactivar(ZONA))
                .isInstanceOf(ReglaNegocioException.class);

        verify(zonaAbiertaRepository, never()).save(any());
    }

    @Test
    void listarPorCampus_porDefectoDevuelveSoloLasActivas() {
        campus(Estado.ACTIVO);
        when(zonaAbiertaRepository.findByCampusIdOrderByNombreAsc(CAMPUS))
                .thenReturn(List.of(zona(Estado.ACTIVO), zona(Estado.INACTIVO)));

        assertThat(zonaAbiertaService.listarPorCampus(CAMPUS, false)).hasSize(1);
        assertThat(zonaAbiertaService.listarPorCampus(CAMPUS, true)).hasSize(2);
    }

    @Test
    void listarPorCampus_campusInexistente_lanzaExcepcion() {
        when(campusRepository.findById(CAMPUS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zonaAbiertaService.listarPorCampus(CAMPUS, false))
                .isInstanceOf(CampusNoEncontradoException.class);
    }

    @Test
    void obtener_noExiste_lanzaExcepcion() {
        when(zonaAbiertaRepository.findById(uuid(99))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zonaAbiertaService.obtener(uuid(99)))
                .isInstanceOf(ZonaAbiertaNoEncontradaException.class);
    }

    @Test
    void inactivar_conZonasSegurasActivas_lanzaReglaDeNegocioYNoGuarda() {
        when(zonaAbiertaRepository.findById(ZONA)).thenReturn(Optional.of(zona(Estado.ACTIVO)));
        when(zonaSeguraRepository.existsByZonaAbiertaIdAndEstado(ZONA, Estado.ACTIVO)).thenReturn(true);

        assertThatThrownBy(() -> zonaAbiertaService.inactivar(ZONA))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("zonas seguras activas");

        verify(zonaAbiertaRepository, never()).save(any());
    }
}
