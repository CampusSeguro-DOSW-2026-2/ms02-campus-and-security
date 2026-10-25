package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaRutaServiceTest {

    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private EdificioGateway edificioGateway;
    @Mock
    private EspacioGateway espacioGateway;
    @Mock
    private ZonaSeguraGateway zonaGateway;

    private ConsultaRutaService service;

    private final UUID edificioId = uuid(10);
    private final UUID espacioId = uuid(11);
    private final UUID zonaId = uuid(12);

    @BeforeEach
    void setUp() {
        service = new ConsultaRutaService(rutaRepository, edificioGateway, espacioGateway, zonaGateway);
    }

    private void edificio(boolean activo) {
        when(edificioGateway.buscarPorId(edificioId))
                .thenReturn(Optional.of(new EdificioInfo(edificioId, "A", "Edificio A", activo)));
    }

    private void espacio() {
        when(espacioGateway.buscarPorId(espacioId))
                .thenReturn(Optional.of(new EspacioInfo(espacioId, edificioId, "Salón 101", true)));
    }

    private void zona(boolean activa) {
        when(zonaGateway.buscarPorId(zonaId))
                .thenReturn(Optional.of(new ZonaInfo(zonaId, "Plazoleta", "Frente al edificio A", activa)));
    }

    private void rutaDelEspacio(EstadoRuta estado) {
        when(rutaRepository.findByEspacioOrigenId(espacioId))
                .thenReturn(Optional.of(
                        new RutaEvacuacion(espacioId, zonaId, "Salir por la puerta norte", estado)));
    }

    @Test
    void consultar_devuelveLaRutaDisponibleConSuDestino() {
        edificio(true);
        espacio();
        zona(true);
        rutaDelEspacio(EstadoRuta.DISPONIBLE);

        List<RutaConsultadaResponse> resultado = service.consultar(edificioId, espacioId);

        assertThat(resultado).hasSize(1);
        RutaConsultadaResponse r = resultado.get(0);
        assertThat(r.tieneRuta()).isTrue();
        assertThat(r.zonaSeguraNombre()).isEqualTo("Plazoleta");
        assertThat(r.recorrido()).isEqualTo("Salir por la puerta norte");
        assertThat(r.estado()).isEqualTo(EstadoRuta.DISPONIBLE);
        assertThat(r.destinoActivo()).isTrue();
        assertThat(r.mensaje()).isNull();
    }

    @Test
    void consultar_sinRutaInformaQueNoExisteRecomendacion() {
        edificio(true);
        espacio();
        when(rutaRepository.findByEspacioOrigenId(espacioId)).thenReturn(Optional.empty());

        RutaConsultadaResponse r = service.consultar(edificioId, espacioId).get(0);

        assertThat(r.tieneRuta()).isFalse();
        assertThat(r.zonaSeguraId()).isNull();
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_RUTA);
    }

    @Test
    void consultar_rutaBloqueadaMuestraElEstadoYUnAviso() {
        edificio(true);
        espacio();
        zona(true);
        rutaDelEspacio(EstadoRuta.BLOQUEADA);

        RutaConsultadaResponse r = service.consultar(edificioId, espacioId).get(0);

        assertThat(r.tieneRuta()).isTrue();
        assertThat(r.estado()).isEqualTo(EstadoRuta.BLOQUEADA);
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_BLOQUEADA);
    }

    @Test
    void consultar_rutaRestringidaMuestraElEstadoYUnAviso() {
        edificio(true);
        espacio();
        zona(true);
        rutaDelEspacio(EstadoRuta.RESTRINGIDA);

        RutaConsultadaResponse r = service.consultar(edificioId, espacioId).get(0);

        assertThat(r.estado()).isEqualTo(EstadoRuta.RESTRINGIDA);
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_RESTRINGIDA);
    }

    @Test
    void consultar_destinoInactivoTienePrioridadSobreElEstado() {
        edificio(true);
        espacio();
        zona(false);
        rutaDelEspacio(EstadoRuta.DISPONIBLE);

        RutaConsultadaResponse r = service.consultar(edificioId, espacioId).get(0);

        assertThat(r.destinoActivo()).isFalse();
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_DESTINO_INACTIVO);
    }

    @Test
    void consultar_destinoInexistenteSeTrataComoInactivo() {
        edificio(true);
        espacio();
        when(zonaGateway.buscarPorId(zonaId)).thenReturn(Optional.empty());
        rutaDelEspacio(EstadoRuta.DISPONIBLE);

        RutaConsultadaResponse r = service.consultar(edificioId, espacioId).get(0);

        assertThat(r.destinoActivo()).isFalse();
        assertThat(r.zonaSeguraNombre()).isNull();
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_DESTINO_INACTIVO);
    }

    @Test
    void consultar_fallaSiElEdificioNoExiste() {
        when(edificioGateway.buscarPorId(edificioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(edificioId, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void consultar_fallaSiElEdificioEstaInactivo() {
        edificio(false);

        assertThatThrownBy(() -> service.consultar(edificioId, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void consultar_fallaSiElEspacioNoExiste() {
        edificio(true);
        when(espacioGateway.buscarPorId(espacioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar(edificioId, espacioId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void consultar_fallaSiElEspacioPerteneceAOtroEdificio() {
        edificio(true);
        when(espacioGateway.buscarPorId(espacioId))
                .thenReturn(Optional.of(new EspacioInfo(espacioId, uuid(13), "Salón 101", true)));

        assertThatThrownBy(() -> service.consultar(edificioId, espacioId))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no pertenece");
    }

    @Test
    void consultar_fallaSiElEspacioNoEstaDisponible() {
        edificio(true);
        when(espacioGateway.buscarPorId(espacioId))
                .thenReturn(Optional.of(new EspacioInfo(espacioId, edificioId, "Salón 101", false)));

        assertThatThrownBy(() -> service.consultar(edificioId, espacioId))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está disponible");
        verify(rutaRepository, never()).findByEspacioOrigenId(any());
    }

    @Test
    void consultar_soloConEdificioDevuelveUnResultadoPorEspacioActivo() {
        UUID otroEspacio = uuid(14);
        UUID inactivo = uuid(15);
        edificio(true);
        when(espacioGateway.listarPorEdificio(edificioId)).thenReturn(List.of(
                new EspacioInfo(espacioId, edificioId, "Salón 101", true),
                new EspacioInfo(otroEspacio, edificioId, "Lab 2", true),
                new EspacioInfo(inactivo, edificioId, "Bodega", false)));
        when(rutaRepository.findByEspacioOrigenIdIn(List.of(espacioId, otroEspacio)))
                .thenReturn(List.of(new RutaEvacuacion(espacioId, zonaId, "Por la puerta norte", EstadoRuta.DISPONIBLE)));
        zona(true);

        List<RutaConsultadaResponse> resultado = service.consultar(edificioId, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).tieneRuta()).isTrue();
        assertThat(resultado.get(1).tieneRuta()).isFalse();
        assertThat(resultado.get(1).mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_RUTA);
    }

    @Test
    void consultar_edificioSinEspaciosDevuelveListaVacia() {
        edificio(true);
        when(espacioGateway.listarPorEdificio(edificioId)).thenReturn(List.of());

        assertThat(service.consultar(edificioId, null)).isEmpty();
    }

    @Test
    void consultarPorEspacio_buscaElEdificioDelEspacioYDevuelveUnSoloResultado() {
        edificio(true);
        espacio();
        zona(true);
        rutaDelEspacio(EstadoRuta.DISPONIBLE);

        RutaConsultadaResponse r = service.consultarPorEspacio(espacioId);

        assertThat(r.espacioId()).isEqualTo(espacioId);
        assertThat(r.tieneRuta()).isTrue();
        assertThat(r.zonaSeguraNombre()).isEqualTo("Plazoleta");
    }

    @Test
    void consultarPorEspacio_fallaSiElEspacioNoExiste() {
        when(espacioGateway.buscarPorId(espacioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarPorEspacio(espacioId))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void consultarPorEdificio_devuelveUnResultadoPorCadaEspacioActivo() {
        edificio(true);
        when(espacioGateway.listarPorEdificio(edificioId)).thenReturn(List.of(
                new EspacioInfo(espacioId, edificioId, "Salón 101", true)));
        when(rutaRepository.findByEspacioOrigenIdIn(List.of(espacioId))).thenReturn(List.of());

        List<RutaConsultadaResponse> resultado = service.consultarPorEdificio(edificioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).tieneRuta()).isFalse();
    }
}
