package edu.eci.dosw.campusseguro.campus.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.service.CodigoQrService.TipoQr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoQrServiceTest {

    private static final UUID ID = uuid(30);
    private static final String BASE = "https://campusseguro.eci.edu.co";

    @Mock
    private EspacioRepository espacioRepository;
    @Mock
    private SalidaRepository salidaRepository;
    @Mock
    private ZonaSeguraRepository zonaSeguraRepository;

    private CodigoQrService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CodigoQrService(BASE + "/", espacioRepository, salidaRepository, zonaSeguraRepository);
    }

    private static String leer(byte[] png) throws Exception {
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap mapa = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
        return new MultiFormatReader().decode(mapa).getText();
    }

    @Test
    void espacio_elQrContieneSoloElEnlacePublicoConSuUuid() throws Exception {
        when(espacioRepository.existsById(ID)).thenReturn(true);

        byte[] png = servicio.generar(TipoQr.ESPACIO, ID);

        assertThat(leer(png)).isEqualTo(BASE + "/api/v1/public/spaces/" + ID);
    }

    @Test
    void salida_elQrApuntaALaConsultaPublicaDeSalidas() throws Exception {
        when(salidaRepository.existsById(ID)).thenReturn(true);

        assertThat(leer(servicio.generar(TipoQr.SALIDA, ID))).isEqualTo(BASE + "/api/v1/public/exits/" + ID);
    }

    @Test
    void zonaSegura_elQrApuntaALaConsultaPublicaDeZonas() throws Exception {
        when(zonaSeguraRepository.existsById(ID)).thenReturn(true);

        assertThat(leer(servicio.generar(TipoQr.ZONA_SEGURA, ID))).isEqualTo(BASE + "/api/v1/public/safe-zones/" + ID);
    }

    @Test
    void generar_devuelveUnPng() {
        when(espacioRepository.existsById(ID)).thenReturn(true);

        byte[] png = servicio.generar(TipoQr.ESPACIO, ID);

        assertThat(png).startsWith(0x89, 'P', 'N', 'G');
    }

    @Test
    void enlace_quitaLaBarraFinalDeLaBase() {
        assertThat(servicio.enlace(TipoQr.SALIDA, ID)).isEqualTo(BASE + "/api/v1/public/exits/" + ID);
    }

    @Test
    void espacioInexistente_falla() {
        when(espacioRepository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> servicio.generar(TipoQr.ESPACIO, ID))
                .isInstanceOf(EspacioNoEncontradoException.class);
    }

    @Test
    void salidaInexistente_falla() {
        when(salidaRepository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> servicio.generar(TipoQr.SALIDA, ID))
                .isInstanceOf(SalidaNoEncontradaException.class);
    }

    @Test
    void zonaSeguraInexistente_falla() {
        when(zonaSeguraRepository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> servicio.generar(TipoQr.ZONA_SEGURA, ID))
                .isInstanceOf(ZonaSeguraNoEncontradaException.class);
    }
}
