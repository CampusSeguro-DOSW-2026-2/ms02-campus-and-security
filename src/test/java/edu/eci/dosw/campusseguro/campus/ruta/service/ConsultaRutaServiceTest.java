package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
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
import static org.mockito.Mockito.when;

// Esta clase cubre la validación de edificio/espacio y el listado por edificio, usando el
// constructor de compatibilidad sin salida/escalera (sin guía automática posible). El contenido de
// la ruta recomendada (disponible, bloqueada, restringida, destino inactivo, etc.) se prueba con el
// servicio completamente conectado en ConsultaRutaGuiaTest, que es la única fuente real de rutas
// desde que se dejó de crear manualmente la ruta espacio -> zona (RF-MS02-05).
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

    @BeforeEach
    void setUp() {
        service = new ConsultaRutaService(rutaRepository, edificioGateway, espacioGateway, zonaGateway);
    }

    private void edificio(boolean activo) {
        when(edificioGateway.buscarPorId(edificioId))
                .thenReturn(Optional.of(new EdificioInfo(edificioId, "Edificio A", activo)));
    }

    private void espacio() {
        when(espacioGateway.buscarPorId(espacioId))
                .thenReturn(Optional.of(new EspacioInfo(espacioId, edificioId, "Salón 101", true)));
    }

    @Test
    void consultar_sinGuiaDisponibleInformaQueNoExisteRecomendacion() {
        edificio(true);
        espacio();

        RutaConsultadaResponse r = service.consultar(edificioId, espacioId).get(0);

        assertThat(r.tieneRuta()).isFalse();
        assertThat(r.zonaSeguraId()).isNull();
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_RUTA);
        assertThat(r.guia()).isNull();
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
    }

    @Test
    void consultar_soloConEdificioDevuelveUnResultadoPorCadaEspacioActivo() {
        UUID otroEspacio = uuid(14);
        UUID inactivo = uuid(15);
        edificio(true);
        when(espacioGateway.listarPorEdificio(edificioId)).thenReturn(List.of(
                new EspacioInfo(espacioId, edificioId, "Salón 101", true),
                new EspacioInfo(otroEspacio, edificioId, "Lab 2", true),
                new EspacioInfo(inactivo, edificioId, "Bodega", false)));

        List<RutaConsultadaResponse> resultado = service.consultar(edificioId, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).tieneRuta()).isFalse();
        assertThat(resultado.get(1).tieneRuta()).isFalse();
        assertThat(resultado.get(0).mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_RUTA);
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

        RutaConsultadaResponse r = service.consultarPorEspacio(espacioId);

        assertThat(r.espacioId()).isEqualTo(espacioId);
        assertThat(r.tieneRuta()).isFalse();
        assertThat(r.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_RUTA);
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

        List<RutaConsultadaResponse> resultado = service.consultarPorEdificio(edificioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).tieneRuta()).isFalse();
    }
}
