package edu.eci.dosw.campusseguro.campus.ruta.service;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.dto.ResumenGeneracionResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.port.CatalogoExterior;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneradorRutasExterioresServiceTest {

    private static final UUID SALIDA = uuid(1);
    private static final UUID OTRA_SALIDA = uuid(2);
    private static final UUID CERCA = uuid(10);
    private static final UUID MEDIA = uuid(11);
    private static final UUID LEJOS = uuid(12);

    @Mock
    private CatalogoExterior catalogo;
    @Mock
    private SalidaGateway salidaGateway;
    @Mock
    private RutaRepository rutaRepository;
    @Mock
    private RutaExteriorService rutaExteriorService;

    private GeneradorRutasExterioresService generador;

    @BeforeEach
    void preparar() {
        generador = new GeneradorRutasExterioresService(catalogo, salidaGateway, rutaRepository,
                rutaExteriorService, new RutasAutoProperties(true, 2));
    }

    private static SalidaInfo salida(UUID id, String lat, String lng, boolean disponible) {
        return new SalidaInfo(id, uuid(100), uuid(101), "Salida " + id, new BigDecimal(lat),
                new BigDecimal(lng), disponible);
    }

    // Las tres zonas están sobre el mismo meridiano que la salida; CERCA < MEDIA < LEJOS.
    private static List<ZonaInfo> zonas() {
        return List.of(
                new ZonaInfo(LEJOS, "Lejos", "u", true, new BigDecimal("4.790000"), new BigDecimal("-74.040000")),
                new ZonaInfo(CERCA, "Cerca", "u", true, new BigDecimal("4.781000"), new BigDecimal("-74.040000")),
                new ZonaInfo(MEDIA, "Media", "u", true, new BigDecimal("4.785000"), new BigDecimal("-74.040000")));
    }

    private static RutaEvacuacion rutaExistente(UUID salida, UUID zona) {
        return RutaEvacuacion.exterior(salida, zona, "existente");
    }

    @Test
    void generarParaSalida_calculaSoloLasZonasMasCercanas() {
        when(salidaGateway.buscarPorId(SALIDA)).thenReturn(Optional.of(salida(SALIDA, "4.780000", "-74.040000", true)));
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(rutaRepository.findBySalidaOrigenId(SALIDA)).thenReturn(List.of());

        ResumenGeneracionResponse resumen = generador.generarParaSalida(SALIDA);

        verify(rutaExteriorService).calcular(SALIDA, CERCA);
        verify(rutaExteriorService).calcular(SALIDA, MEDIA);
        verify(rutaExteriorService, never()).calcular(SALIDA, LEJOS);
        assertThat(resumen).isEqualTo(new ResumenGeneracionResponse(2, 0, false));
    }

    @Test
    void generarParaSalida_recalculaTambienLasRutasQueYaTenia() {
        when(salidaGateway.buscarPorId(SALIDA)).thenReturn(Optional.of(salida(SALIDA, "4.780000", "-74.040000", true)));
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(rutaRepository.findBySalidaOrigenId(SALIDA)).thenReturn(List.of(rutaExistente(SALIDA, LEJOS)));

        ResumenGeneracionResponse resumen = generador.generarParaSalida(SALIDA);

        verify(rutaExteriorService).calcular(SALIDA, LEJOS);
        assertThat(resumen.generadas()).isEqualTo(3);
    }

    @Test
    void generarParaSalida_noDisponible_noHaceNada() {
        when(salidaGateway.buscarPorId(SALIDA)).thenReturn(Optional.of(salida(SALIDA, "4.780000", "-74.040000", false)));

        ResumenGeneracionResponse resumen = generador.generarParaSalida(SALIDA);

        assertThat(resumen).isEqualTo(ResumenGeneracionResponse.vacio());
        verify(rutaExteriorService, never()).calcular(any(), any());
    }

    @Test
    void generarParaSalida_inexistente_noHaceNada() {
        when(salidaGateway.buscarPorId(SALIDA)).thenReturn(Optional.empty());

        assertThat(generador.generarParaSalida(SALIDA)).isEqualTo(ResumenGeneracionResponse.vacio());
        verify(rutaExteriorService, never()).calcular(any(), any());
    }

    @Test
    void generarParaSalida_unParQueFalla_seOmiteYSigueConLosDemas() {
        when(salidaGateway.buscarPorId(SALIDA)).thenReturn(Optional.of(salida(SALIDA, "4.780000", "-74.040000", true)));
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(rutaRepository.findBySalidaOrigenId(SALIDA)).thenReturn(List.of());
        doThrow(new ReglaNegocioException("sin recorrido")).when(rutaExteriorService).calcular(SALIDA, CERCA);

        ResumenGeneracionResponse resumen = generador.generarParaSalida(SALIDA);

        verify(rutaExteriorService).calcular(SALIDA, MEDIA);
        assertThat(resumen).isEqualTo(new ResumenGeneracionResponse(1, 1, false));
    }

    @Test
    void generarParaSalida_siElProveedorCae_seInterrumpe() {
        when(salidaGateway.buscarPorId(SALIDA)).thenReturn(Optional.of(salida(SALIDA, "4.780000", "-74.040000", true)));
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(rutaRepository.findBySalidaOrigenId(SALIDA)).thenReturn(List.of());
        doThrow(new NavegacionNoDisponibleException("caído", null)).when(rutaExteriorService).calcular(SALIDA, CERCA);

        ResumenGeneracionResponse resumen = generador.generarParaSalida(SALIDA);

        verify(rutaExteriorService, never()).calcular(SALIDA, MEDIA);
        assertThat(resumen).isEqualTo(new ResumenGeneracionResponse(0, 2, true));
    }

    @Test
    void generarParaZona_laConectaConLasSalidasParaLasQueEsCercana() {
        // Con max=2: para SALIDA las cercanas son CERCA y MEDIA; para OTRA_SALIDA (al norte) son LEJOS y MEDIA.
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(catalogo.salidasDisponibles()).thenReturn(List.of(
                salida(SALIDA, "4.780000", "-74.040000", true),
                salida(OTRA_SALIDA, "4.795000", "-74.040000", true)));
        when(rutaRepository.findByZonaSeguraDestinoIdAndSalidaOrigenIdNotNull(CERCA)).thenReturn(List.of());

        ResumenGeneracionResponse resumen = generador.generarParaZona(CERCA);

        verify(rutaExteriorService).calcular(SALIDA, CERCA);
        verify(rutaExteriorService, never()).calcular(OTRA_SALIDA, CERCA);
        assertThat(resumen.generadas()).isEqualTo(1);
    }

    @Test
    void generarParaZona_recalculaLasRutasQueYaLlegabanAEsaZona() {
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(catalogo.salidasDisponibles()).thenReturn(List.of(salida(SALIDA, "4.780000", "-74.040000", true)));
        when(rutaRepository.findByZonaSeguraDestinoIdAndSalidaOrigenIdNotNull(LEJOS))
                .thenReturn(List.of(rutaExistente(OTRA_SALIDA, LEJOS)));

        generador.generarParaZona(LEJOS);

        verify(rutaExteriorService).calcular(OTRA_SALIDA, LEJOS);
        verify(rutaExteriorService, never()).calcular(SALIDA, LEJOS);
    }

    @Test
    void generarParaZona_sinCoordenadasOInactiva_noHaceNada() {
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(List.of());

        assertThat(generador.generarParaZona(CERCA)).isEqualTo(ResumenGeneracionResponse.vacio());
        verify(rutaExteriorService, never()).calcular(any(), any());
    }

    @Test
    void generarTodas_porDefectoSoloCreaLasQueFaltan() {
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(catalogo.salidasDisponibles()).thenReturn(List.of(salida(SALIDA, "4.780000", "-74.040000", true)));
        when(rutaRepository.findBySalidaOrigenIdIn(List.of(SALIDA)))
                .thenReturn(List.of(rutaExistente(SALIDA, CERCA)));

        ResumenGeneracionResponse resumen = generador.generarTodas(false);

        verify(rutaExteriorService, never()).calcular(SALIDA, CERCA);
        verify(rutaExteriorService).calcular(SALIDA, MEDIA);
        assertThat(resumen).isEqualTo(new ResumenGeneracionResponse(1, 0, false));
    }

    @Test
    void generarTodas_conRecalcularRehaceTambienLasExistentes() {
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(catalogo.salidasDisponibles()).thenReturn(List.of(salida(SALIDA, "4.780000", "-74.040000", true)));

        ResumenGeneracionResponse resumen = generador.generarTodas(true);

        verify(rutaExteriorService).calcular(SALIDA, CERCA);
        verify(rutaExteriorService).calcular(SALIDA, MEDIA);
        verify(rutaRepository, never()).findBySalidaOrigenIdIn(any());
        assertThat(resumen.generadas()).isEqualTo(2);
    }

    @Test
    void generarTodas_sinSalidasOSinZonas_devuelveVacio() {
        when(catalogo.zonasActivasConCoordenadas()).thenReturn(zonas());
        when(catalogo.salidasDisponibles()).thenReturn(List.of());

        assertThat(generador.generarTodas(false)).isEqualTo(ResumenGeneracionResponse.vacio());
    }
}
