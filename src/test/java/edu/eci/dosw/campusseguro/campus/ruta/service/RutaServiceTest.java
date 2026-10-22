package edu.eci.dosw.campusseguro.campus.ruta.service;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.CrearRutaRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaDuplicadaException;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RutaServiceTest {

    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private EspacioGateway espacioGateway;
    @Mock
    private ZonaSeguraGateway zonaGateway;

    private RutaService service;

    private final Long edificioId = 10L;
    private final Long espacioId = 11L;
    private final Long zonaId = 12L;

    @BeforeEach
    void setUp() {
        service = new RutaService(rutaRepository, espacioGateway, zonaGateway);
    }

    private CrearRutaRequest request() {
        return new CrearRutaRequest(espacioId, zonaId, "  Salir por la puerta norte  ", EstadoRuta.DISPONIBLE, null);
    }

    private void espacioActivo(boolean activo) {
        when(espacioGateway.buscarPorId(espacioId))
                .thenReturn(Optional.of(new EspacioInfo(espacioId, edificioId, "Salón 101", activo)));
    }

    private void zonaActiva(boolean activa) {
        when(zonaGateway.buscarPorId(zonaId))
                .thenReturn(Optional.of(new ZonaInfo(zonaId, "Plazoleta", "Frente al edificio A", activa)));
    }

    @Test
    void crear_guardaLaRutaConDescripcionSinEspacios() {
        espacioActivo(true);
        zonaActiva(true);
        when(rutaRepository.existsByEspacioOrigenId(espacioId)).thenReturn(false);
        when(rutaRepository.saveAndFlush(any(RutaEvacuacion.class))).thenAnswer(i -> i.getArgument(0));

        RutaResponse respuesta = service.crear(request());

        assertThat(respuesta.espacioOrigenId()).isEqualTo(espacioId);
        assertThat(respuesta.zonaSeguraDestinoId()).isEqualTo(zonaId);
        assertThat(respuesta.descripcion()).isEqualTo("Salir por la puerta norte");
        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.DISPONIBLE);
    }

    @Test
    void crear_fallaSiElEspacioNoExiste() {
        when(espacioGateway.buscarPorId(espacioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("espacio");
        verify(rutaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_fallaSiElEspacioEstaInactivo() {
        espacioActivo(false);

        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");
        verify(rutaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_fallaSiLaZonaNoExiste() {
        espacioActivo(true);
        when(zonaGateway.buscarPorId(zonaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("zona segura");
    }

    @Test
    void crear_fallaSiLaZonaEstaInactiva() {
        espacioActivo(true);
        zonaActiva(false);

        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void crear_fallaSiElEspacioYaTieneRuta() {
        espacioActivo(true);
        zonaActiva(true);
        when(rutaRepository.existsByEspacioOrigenId(espacioId)).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(RutaDuplicadaException.class);
        verify(rutaRepository, never()).saveAndFlush(any());
    }

    @Test
    void crear_traduceLaViolacionDeUnicidadEnCondicionDeCarrera() {
        espacioActivo(true);
        zonaActiva(true);
        when(rutaRepository.existsByEspacioOrigenId(espacioId)).thenReturn(false);
        when(rutaRepository.saveAndFlush(any(RutaEvacuacion.class))).thenThrow(new DataIntegrityViolationException("uk_ruta_espacio"));

        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(RutaDuplicadaException.class);
    }

    @Test
    void cambiarEstado_actualizaLaRuta() {
        RutaEvacuacion ruta = new RutaEvacuacion(espacioId, zonaId, null, "Ruta", EstadoRuta.DISPONIBLE);
        ReflectionTestUtils.setField(ruta, "id", 7L);
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));
        when(rutaRepository.save(ruta)).thenReturn(ruta);

        RutaResponse respuesta = service.cambiarEstado(ruta.getId(), EstadoRuta.BLOQUEADA);

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.BLOQUEADA);
        verify(rutaRepository).save(ruta);
    }

    @Test
    void cambiarEstado_fallaSiLaRutaNoExiste() {
        Long id = 13L;
        when(rutaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(id, EstadoRuta.RESTRINGIDA))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void obtener_devuelveLaRuta() {
        RutaEvacuacion ruta = new RutaEvacuacion(espacioId, zonaId, null, "Ruta", EstadoRuta.RESTRINGIDA);
        ReflectionTestUtils.setField(ruta, "id", 8L);
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        assertThat(service.obtener(ruta.getId()).estado()).isEqualTo(EstadoRuta.RESTRINGIDA);
    }

    @Test
    void listar_devuelveTodasLasRutas() {
        when(rutaRepository.findAll()).thenReturn(List.of(
                new RutaEvacuacion(espacioId, zonaId, null, "A", EstadoRuta.DISPONIBLE),
                new RutaEvacuacion(14L, zonaId, null, "B", EstadoRuta.BLOQUEADA)));

        assertThat(service.listar()).hasSize(2);
    }

    @Test
    void crear_aceptaUnaRutaAlternativaExistente() {
        Long alternativa = 15L;
        espacioActivo(true);
        zonaActiva(true);
        when(rutaRepository.existsById(alternativa)).thenReturn(true);
        when(rutaRepository.existsByEspacioOrigenId(espacioId)).thenReturn(false);
        when(rutaRepository.saveAndFlush(any(RutaEvacuacion.class))).thenAnswer(i -> i.getArgument(0));

        RutaResponse respuesta = service.crear(
                new CrearRutaRequest(espacioId, zonaId, "Por la puerta sur", EstadoRuta.DISPONIBLE, alternativa));

        assertThat(respuesta.rutaAlternativaId()).isEqualTo(alternativa);
    }

    @Test
    void crear_fallaSiLaRutaAlternativaNoExiste() {
        Long alternativa = 16L;
        espacioActivo(true);
        zonaActiva(true);
        when(rutaRepository.existsById(alternativa)).thenReturn(false);

        assertThatThrownBy(() -> service.crear(
                new CrearRutaRequest(espacioId, zonaId, "Por la puerta sur", EstadoRuta.DISPONIBLE, alternativa)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("alternativa");
        verify(rutaRepository, never()).saveAndFlush(any());
    }
}
