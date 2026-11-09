package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.GuiaEvacuacionResponse;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EscaleraGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EscaleraGateway.EscaleraInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// Guía completa desde un espacio: escalera + salida + ruta exterior (PR 5).
@ExtendWith(MockitoExtension.class)
class ConsultaRutaGuiaTest {

    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private EdificioGateway edificioGateway;
    @Mock
    private EspacioGateway espacioGateway;
    @Mock
    private ZonaSeguraGateway zonaGateway;
    @Mock
    private SalidaGateway salidaGateway;
    @Mock
    private EscaleraGateway escaleraGateway;

    private ConsultaRutaService service;

    private final UUID edificioId = uuid(10);
    private final UUID espacioId = uuid(11);
    private final UUID zonaId = uuid(12);
    private final UUID piso3 = uuid(23);
    private final UUID piso1 = uuid(21);
    private final UUID salidaId = uuid(30);

    @BeforeEach
    void setUp() {
        service = new ConsultaRutaService(rutaRepository, edificioGateway, espacioGateway, zonaGateway,
                null, salidaGateway, escaleraGateway);
        EspacioInfo salon = new EspacioInfo(espacioId, edificioId, "Salón 301", true, null, piso3, 3);
        when(edificioGateway.buscarPorId(edificioId))
                .thenReturn(Optional.of(new EdificioInfo(edificioId, "Edificio A", true)));
        when(espacioGateway.buscarPorId(espacioId)).thenReturn(Optional.of(salon));
        lenient().when(rutaRepository.findByEspacioOrigenId(espacioId)).thenReturn(Optional.empty());
        lenient().when(zonaGateway.buscarPorId(zonaId))
                .thenReturn(Optional.of(new ZonaInfo(zonaId, "Plazoleta", "Frente", true)));
    }

    private SalidaInfo salida(UUID id, UUID pisoId, boolean disponible) {
        return new SalidaInfo(id, edificioId, pisoId, "Puerta " + id.getLeastSignificantBits(),
                new BigDecimal("4.782710"), new BigDecimal("-74.042630"), disponible);
    }

    private RutaEvacuacion rutaExterior(UUID salida, double metros) {
        RutaEvacuacion r = RutaEvacuacion.exterior(salida, zonaId, "Ruta exterior");
        r.registrarCalculo(metros, metros / 1.4, "4.78,-74.04;4.79,-74.05", "1.0|1.0|Siga recto", Instant.EPOCH);
        return r;
    }

    private EscaleraInfo escalera(String codigo) {
        return new EscaleraInfo(uuid(40), codigo, "Escalera " + codigo, edificioId, List.of(piso3, piso1));
    }

    @Test
    void guia_conEscaleraDisponible_indicaEscaleraSalidaYRutaExterior() {
        when(salidaGateway.listarPorEdificio(edificioId)).thenReturn(List.of(salida(salidaId, piso1, true)));
        when(rutaRepository.findBySalidaOrigenIdIn(any())).thenReturn(List.of(rutaExterior(salidaId, 280)));
        when(escaleraGateway.listarDisponiblesQueConectan(edificioId, piso3, piso1))
                .thenReturn(List.of(escalera("E-1")));

        RutaConsultadaResponse respuesta = service.consultarPorEspacio(espacioId);

        GuiaEvacuacionResponse guia = respuesta.guia();
        assertThat(respuesta.tieneRuta()).isFalse();
        assertThat(guia.edificioNombre()).isEqualTo("Edificio A");
        assertThat(guia.pisoNumero()).isEqualTo(3);
        assertThat(guia.requiereEscalera()).isTrue();
        assertThat(guia.escaleras()).extracting("codigo").containsExactly("E-1");
        assertThat(guia.salida().id()).isEqualTo(salidaId);
        assertThat(guia.rutaExterior().distanciaMetros()).isEqualByComparingTo("280.00");
        assertThat(guia.zonaSeguraNombre()).isEqualTo("Plazoleta");
        assertThat(guia.indicacion()).contains("escalera Escalera E-1").contains("Plazoleta");
        assertThat(guia.mensaje()).isNull();
    }

    @Test
    void guia_siLaSalidaEstaEnElMismoPiso_noPideEscalera() {
        when(salidaGateway.listarPorEdificio(edificioId)).thenReturn(List.of(salida(salidaId, piso3, true)));
        when(rutaRepository.findBySalidaOrigenIdIn(any())).thenReturn(List.of(rutaExterior(salidaId, 100)));

        GuiaEvacuacionResponse guia = service.consultarPorEspacio(espacioId).guia();

        assertThat(guia.requiereEscalera()).isFalse();
        assertThat(guia.escaleras()).isEmpty();
        assertThat(guia.indicacion()).startsWith("Salga por");
    }

    @Test
    void guia_sinEscalerasDisponibles_avisaYNoRecomiendaSalida() {
        when(salidaGateway.listarPorEdificio(edificioId)).thenReturn(List.of(salida(salidaId, piso1, true)));
        when(rutaRepository.findBySalidaOrigenIdIn(any())).thenReturn(List.of(rutaExterior(salidaId, 280)));
        when(escaleraGateway.listarDisponiblesQueConectan(edificioId, piso3, piso1)).thenReturn(List.of());

        GuiaEvacuacionResponse guia = service.consultarPorEspacio(espacioId).guia();

        assertThat(guia.salida()).isNull();
        assertThat(guia.rutaExterior()).isNull();
        assertThat(guia.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_ESCALERA);
    }

    @Test
    void guia_sinSalidaConRutaExterior_avisa() {
        when(salidaGateway.listarPorEdificio(edificioId)).thenReturn(List.of(salida(salidaId, piso1, true)));
        when(rutaRepository.findBySalidaOrigenIdIn(any())).thenReturn(List.of());

        GuiaEvacuacionResponse guia = service.consultarPorEspacio(espacioId).guia();

        assertThat(guia.salida()).isNull();
        assertThat(guia.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_SALIDA);
    }

    @Test
    void guia_ignoraSalidasNoDisponibles() {
        when(salidaGateway.listarPorEdificio(edificioId)).thenReturn(List.of(salida(salidaId, piso3, false)));

        GuiaEvacuacionResponse guia = service.consultarPorEspacio(espacioId).guia();

        assertThat(guia.salida()).isNull();
        assertThat(guia.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_SALIDA);
    }

    @Test
    void guia_conVariasSalidas_eligeLaDeRecorridoMasCorto() {
        UUID otraSalida = uuid(31);
        when(salidaGateway.listarPorEdificio(edificioId))
                .thenReturn(List.of(salida(salidaId, piso3, true), salida(otraSalida, piso3, true)));
        when(rutaRepository.findBySalidaOrigenIdIn(any()))
                .thenReturn(List.of(rutaExterior(salidaId, 500), rutaExterior(otraSalida, 150)));

        GuiaEvacuacionResponse guia = service.consultarPorEspacio(espacioId).guia();

        assertThat(guia.salida().id()).isEqualTo(otraSalida);
        assertThat(guia.rutaExterior().distanciaMetros()).isEqualByComparingTo("150.00");
    }

    @Test
    void guia_conZonaDestinoInactiva_noLaRecomienda() {
        when(zonaGateway.buscarPorId(zonaId))
                .thenReturn(Optional.of(new ZonaInfo(zonaId, "Plazoleta", "Frente", false)));
        when(salidaGateway.listarPorEdificio(edificioId)).thenReturn(List.of(salida(salidaId, piso3, true)));
        when(rutaRepository.findBySalidaOrigenIdIn(any())).thenReturn(List.of(rutaExterior(salidaId, 100)));

        GuiaEvacuacionResponse guia = service.consultarPorEspacio(espacioId).guia();

        assertThat(guia.salida()).isNull();
        assertThat(guia.mensaje()).isEqualTo(ConsultaRutaService.MSG_SIN_SALIDA);
    }

    @Test
    void sinGatewaysDeGuia_laRespuestaNoTraeGuia() {
        ConsultaRutaService sinGuia = new ConsultaRutaService(rutaRepository, edificioGateway, espacioGateway, zonaGateway);

        assertThat(sinGuia.consultarPorEspacio(espacioId).guia()).isNull();
    }
}
