package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

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
import edu.eci.dosw.campusseguro.campus.ruta.dto.ActualizarRutaRequest;
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

    private final UUID edificioId = uuid(10);
    private final UUID espacioId = uuid(11);
    private final UUID zonaId = uuid(12);

    @BeforeEach
    void setUp() {
        service = new RutaService(rutaRepository, espacioGateway, zonaGateway);
    }

    private CrearRutaRequest request() {
        return new CrearRutaRequest(
                espacioId,
                zonaId,
                "Salir por la puerta norte",
                EstadoRuta.DISPONIBLE,
                null);
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
     void actualizar_modificaDescripcionYDestino() {
        UUID nuevaZonaId = uuid(20);
        RutaEvacuacion ruta = new RutaEvacuacion(
                espacioId, zonaId, "Ruta anterior", EstadoRuta.DISPONIBLE);
        ReflectionTestUtils.setField(ruta, "id", uuid(19));

        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));
        when(zonaGateway.buscarPorId(nuevaZonaId))
                .thenReturn(Optional.of(new ZonaInfo(
                        nuevaZonaId, "Patio central", "Bloque A", true)));
        when(rutaRepository.save(ruta)).thenReturn(ruta);

        RutaResponse respuesta = service.actualizar(
                ruta.getId(),
                new ActualizarRutaRequest(nuevaZonaId, "  Ruta actualizada  "));

        assertThat(respuesta.zonaSeguraDestinoId()).isEqualTo(nuevaZonaId);
        assertThat(respuesta.descripcion()).isEqualTo("Ruta actualizada");
        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.DISPONIBLE);
        assertThat(respuesta.espacioOrigenId()).isEqualTo(espacioId);
    }

    @Test
    void actualizar_fallaSiLaRutaNoExiste() {
        UUID rutaId = uuid(21);
        when(rutaRepository.findById(rutaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                rutaId, new ActualizarRutaRequest(zonaId, "Nueva descripción")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(rutaRepository, never()).save(any());
    }

    @Test
    void actualizar_fallaSiLaZonaNoExiste() {
        UUID rutaId = uuid(22);
        RutaEvacuacion ruta = new RutaEvacuacion(
                espacioId, zonaId, "Ruta anterior", EstadoRuta.DISPONIBLE);
        ReflectionTestUtils.setField(ruta, "id", rutaId);

        UUID zonaInexistente = uuid(23);
        when(rutaRepository.findById(rutaId)).thenReturn(Optional.of(ruta));
        when(zonaGateway.buscarPorId(zonaInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(
                rutaId, new ActualizarRutaRequest(zonaInexistente, "Nueva descripción")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(rutaRepository, never()).save(any());
    }

    @Test
    void actualizar_fallaSiLaZonaEstaInactiva() {
        UUID rutaId = uuid(24);
        RutaEvacuacion ruta = new RutaEvacuacion(
                espacioId, zonaId, "Ruta anterior", EstadoRuta.DISPONIBLE);
        ReflectionTestUtils.setField(ruta, "id", rutaId);

        UUID zonaInactiva = uuid(25);
        when(rutaRepository.findById(rutaId)).thenReturn(Optional.of(ruta));
        when(zonaGateway.buscarPorId(zonaInactiva))
                .thenReturn(Optional.of(new ZonaInfo(
                        zonaInactiva, "Zona cerrada", "Bloque B", false)));

        assertThatThrownBy(() -> service.actualizar(
                rutaId, new ActualizarRutaRequest(zonaInactiva, "Nueva descripción")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactiva");

        verify(rutaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_actualizaLaRuta() {
        RutaEvacuacion ruta = new RutaEvacuacion(espacioId, zonaId, "Ruta", EstadoRuta.DISPONIBLE);
        ReflectionTestUtils.setField(ruta, "id", uuid(7));
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));
        when(rutaRepository.save(ruta)).thenReturn(ruta);

        RutaResponse respuesta = service.cambiarEstado(ruta.getId(), EstadoRuta.BLOQUEADA, "  Obra en la escalera  ");

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.BLOQUEADA);
        assertThat(respuesta.motivo()).isEqualTo("Obra en la escalera");
        verify(rutaRepository).save(ruta);
    }

    @Test
    void cambiarEstado_fallaSiLaRutaNoExiste() {
        UUID id = uuid(13);
        when(rutaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(id, EstadoRuta.RESTRINGIDA, "Paso parcial"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private RutaEvacuacion rutaBloqueada() {
        RutaEvacuacion ruta = new RutaEvacuacion(espacioId, zonaId, "Ruta", EstadoRuta.DISPONIBLE);
        ruta.cambiarEstado(EstadoRuta.BLOQUEADA, "Obra en la escalera");
        ReflectionTestUtils.setField(ruta, "id", uuid(9));
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));
        return ruta;
    }

    @Test
    void cambiarEstado_aBloqueadaSinMotivo_seRechaza() {
        RutaEvacuacion ruta = rutaBloqueada();

        assertThatThrownBy(() -> service.cambiarEstado(ruta.getId(), EstadoRuta.BLOQUEADA, "  "))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("motivo");
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aDisponible_borraElMotivo() {
        RutaEvacuacion ruta = rutaBloqueada();
        espacioActivo(true);
        zonaActiva(true);
        when(rutaRepository.save(ruta)).thenReturn(ruta);

        RutaResponse respuesta = service.cambiarEstado(ruta.getId(), EstadoRuta.DISPONIBLE, "se ignora");

        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.DISPONIBLE);
        assertThat(respuesta.motivo()).isNull();
    }

    @Test
    void cambiarEstado_aDisponibleConEspacioNoDisponible_seRechaza() {
        RutaEvacuacion ruta = rutaBloqueada();
        espacioActivo(false);

        assertThatThrownBy(() -> service.cambiarEstado(ruta.getId(), EstadoRuta.DISPONIBLE, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("espacio de origen no está disponible");
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aRestringidaConZonaInactiva_seRechaza() {
        RutaEvacuacion ruta = rutaBloqueada();
        espacioActivo(true);
        zonaActiva(false);

        assertThatThrownBy(() -> service.cambiarEstado(ruta.getId(), EstadoRuta.RESTRINGIDA, "Paso parcial"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("zona segura de destino está inactiva");
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_aBloqueada_noRevisaEspacioNiZona() {
        RutaEvacuacion ruta = rutaBloqueada();
        when(rutaRepository.save(ruta)).thenReturn(ruta);

        service.cambiarEstado(ruta.getId(), EstadoRuta.BLOQUEADA, "Sismo");

        verify(espacioGateway, never()).buscarPorId(any());
        verify(zonaGateway, never()).buscarPorId(any());
    }

    @Test
    void crear_bloqueadaSinMotivo_seRechaza() {
        espacioActivo(true);
        zonaActiva(true);

        assertThatThrownBy(() -> service.crear(
                new CrearRutaRequest(espacioId, zonaId, "Por la puerta sur", EstadoRuta.BLOQUEADA, null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("motivo");
        verify(rutaRepository, never()).saveAndFlush(any());
    }

    @Test
    void obtener_devuelveLaRuta() {
        RutaEvacuacion ruta = new RutaEvacuacion(espacioId, zonaId, "Ruta", EstadoRuta.RESTRINGIDA);
        ReflectionTestUtils.setField(ruta, "id", uuid(8));
        when(rutaRepository.findById(ruta.getId())).thenReturn(Optional.of(ruta));

        assertThat(service.obtener(ruta.getId()).estado()).isEqualTo(EstadoRuta.RESTRINGIDA);
    }

    @Test
    void listar_devuelveTodasLasRutas() {
        when(rutaRepository.findAll()).thenReturn(List.of(
                new RutaEvacuacion(espacioId, zonaId, "A", EstadoRuta.DISPONIBLE),
                new RutaEvacuacion(uuid(14), zonaId, "B", EstadoRuta.BLOQUEADA)));

        assertThat(service.listar()).hasSize(2);
    }
}
