package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaExteriorResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.navegacion.FakeProveedorNavegacion;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RutaExteriorServiceTest {

    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private SalidaGateway salidaGateway;
    @Mock
    private ZonaSeguraGateway zonaGateway;

    private FakeProveedorNavegacion proveedor;
    private RutaExteriorService service;

    private final UUID salidaId = uuid(30);
    private final UUID zonaId = uuid(31);

    @BeforeEach
    void setUp() {
        proveedor = new FakeProveedorNavegacion();
        service = new RutaExteriorService(rutaRepository, salidaGateway, zonaGateway, proveedor);
    }

    private void salida(boolean disponible) {
        when(salidaGateway.buscarPorId(salidaId)).thenReturn(Optional.of(new SalidaInfo(
                salidaId, uuid(10), uuid(2), "Puerta principal",
                new BigDecimal("4.782710"), new BigDecimal("-74.042630"), disponible)));
    }

    private void zona(boolean activa, BigDecimal lat, BigDecimal lng) {
        when(zonaGateway.buscarPorId(zonaId))
                .thenReturn(Optional.of(new ZonaInfo(zonaId, "Plazoleta", "Frente al edificio", activa, lat, lng)));
    }

    @Test
    void calcular_creaLaRutaYGuardaElRecorrido() {
        salida(true);
        zona(true, new BigDecimal("4.783000"), new BigDecimal("-74.043000"));
        when(rutaRepository.findBySalidaOrigenIdAndZonaSeguraDestinoId(salidaId, zonaId))
                .thenReturn(Optional.empty());
        when(rutaRepository.save(any(RutaEvacuacion.class))).thenAnswer(i -> i.getArgument(0));

        RutaExteriorResponse respuesta = service.calcular(salidaId, zonaId);

        assertThat(respuesta.salidaOrigenId()).isEqualTo(salidaId);
        assertThat(respuesta.zonaSeguraDestinoId()).isEqualTo(zonaId);
        assertThat(respuesta.estado()).isEqualTo(EstadoRuta.DISPONIBLE);
        assertThat(respuesta.distanciaMetros()).isPositive();
        assertThat(respuesta.geometria()).hasSize(2);
        assertThat(respuesta.pasos()).hasSize(2);
        assertThat(proveedor.llamadas()).isEqualTo(1);
    }

    @Test
    void calcular_siElParYaTieneRuta_laRecalculaSinDuplicarla() {
        salida(true);
        zona(true, new BigDecimal("4.783000"), new BigDecimal("-74.043000"));
        RutaEvacuacion existente = RutaEvacuacion.exterior(salidaId, zonaId, "vieja");
        existente.registrarCalculo(1, 1, "0,0;1,1", "1.0|1.0|Inicio", java.time.Instant.EPOCH);
        when(rutaRepository.findBySalidaOrigenIdAndZonaSeguraDestinoId(salidaId, zonaId))
                .thenReturn(Optional.of(existente));
        when(rutaRepository.save(any(RutaEvacuacion.class))).thenAnswer(i -> i.getArgument(0));

        RutaExteriorResponse respuesta = service.calcular(salidaId, zonaId);

        assertThat(respuesta.distanciaMetros()).isGreaterThan(new BigDecimal("1.00"));
        verify(rutaRepository).save(existente);
    }

    @Test
    void calcular_salidaInexistente_lanza404() {
        when(salidaGateway.buscarPorId(salidaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(salidaId, zonaId))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(proveedor.llamadas()).isZero();
    }

    @Test
    void calcular_salidaNoDisponible_lanzaReglaDeNegocio() {
        salida(false);

        assertThatThrownBy(() -> service.calcular(salidaId, zonaId))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está disponible");
        assertThat(proveedor.llamadas()).isZero();
    }

    @Test
    void calcular_zonaInexistente_lanza404() {
        salida(true);
        when(zonaGateway.buscarPorId(zonaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcular(salidaId, zonaId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void calcular_zonaInactiva_lanzaReglaDeNegocio() {
        salida(true);
        zona(false, new BigDecimal("4.783000"), new BigDecimal("-74.043000"));

        assertThatThrownBy(() -> service.calcular(salidaId, zonaId))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void calcular_zonaSinCoordenadas_noPuedeSerDestino() {
        salida(true);
        zona(true, null, null);

        assertThatThrownBy(() -> service.calcular(salidaId, zonaId))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no tiene coordenadas");
        assertThat(proveedor.llamadas()).isZero();
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void calcular_siElProveedorFalla_noGuardaNada() {
        service = new RutaExteriorService(rutaRepository, salidaGateway, zonaGateway,
                FakeProveedorNavegacion.fallandoCon(new NavegacionNoDisponibleException("caído")));
        salida(true);
        zona(true, new BigDecimal("4.783000"), new BigDecimal("-74.043000"));

        assertThatThrownBy(() -> service.calcular(salidaId, zonaId))
                .isInstanceOf(NavegacionNoDisponibleException.class);
        verify(rutaRepository, never()).save(any());
    }

    @Test
    void listarPorSalida_devuelveLosRecorridosGuardados() {
        salida(true);
        RutaEvacuacion ruta = RutaEvacuacion.exterior(salidaId, zonaId, "Ruta exterior");
        ruta.registrarCalculo(120.5, 90, "4.78,-74.04;4.79,-74.05", "120.5|90.0|Siga recto", java.time.Instant.EPOCH);
        when(rutaRepository.findBySalidaOrigenId(salidaId)).thenReturn(List.of(ruta));

        List<RutaExteriorResponse> rutas = service.listarPorSalida(salidaId);

        assertThat(rutas).hasSize(1);
        assertThat(rutas.get(0).distanciaMetros()).isEqualByComparingTo("120.50");
        assertThat(rutas.get(0).pasos().get(0).instruccion()).isEqualTo("Siga recto");
    }

    @Test
    void listarPorSalida_salidaInexistente_lanza404() {
        when(salidaGateway.buscarPorId(salidaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorSalida(salidaId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
