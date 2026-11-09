package edu.eci.dosw.campusseguro.campus.service;

import java.util.Optional;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Imagen;
import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.ImagenRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImagenServiceTest {

    private static final UUID EDIFICIO_ID = UUID.randomUUID();

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    @Mock
    private ImagenRepository imagenRepository;

    @Mock
    private EdificioRepository edificioRepository;

    @Mock
    private EspacioRepository espacioRepository;

    @Mock
    private SalidaRepository salidaRepository;

    @Mock
    private ZonaSeguraRepository zonaSeguraRepository;

    @InjectMocks
    private ImagenService imagenService;

    @BeforeEach
    void configurarTamanioMaximo() {
        ReflectionTestUtils.setField(imagenService, "tamanioMaximo", 5 * 1024 * 1024L);
    }

    private void edificio(Estado estado) {
        Edificio edificio = new Edificio();
        edificio.setId(EDIFICIO_ID);
        edificio.setNombre("Bloque H");
        edificio.setEstado(estado);

        when(edificioRepository.findById(EDIFICIO_ID))
                .thenReturn(Optional.of(edificio));
    }

    @Test
    void registrar_pngValido_guardaMetadatosYGeneraRutaSegura() {
        edificio(Estado.ACTIVO);

        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Imagen resultado = imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "../../foto.jpg",
                PNG
        );

        assertThat(resultado.getTipoContenido()).isEqualTo("image/png");
        assertThat(resultado.getTamanioBytes()).isEqualTo(8L);
        assertThat(resultado.getNombreOriginal()).isEqualTo("../../foto.jpg");
        assertThat(resultado.getRutaArchivo())
                .startsWith("imagenes/")
                .endsWith(".png")
                .doesNotContain("foto.jpg")
                .doesNotContain("..");

        verify(imagenRepository).save(any(Imagen.class));
    }

    @Test
    void registrar_jpegValido_detectaFormatoPorContenido() {
        edificio(Estado.ACTIVO);

        byte[] jpeg = {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
        };

        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Imagen resultado = imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                jpeg
        );

        assertThat(resultado.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(resultado.getRutaArchivo()).endsWith(".jpg");
    }

    @Test
    void registrar_webpValido_detectaFormatoPorContenido() {
        edificio(Estado.ACTIVO);

        byte[] webp = {
                'R', 'I', 'F', 'F',
                0, 0, 0, 0,
                'W', 'E', 'B', 'P'
        };

        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Imagen resultado = imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.webp",
                webp
        );

        assertThat(resultado.getTipoContenido()).isEqualTo("image/webp");
        assertThat(resultado.getRutaArchivo()).endsWith(".webp");
    }

    @Test
    void registrar_conExtensionPermitidaPeroContenidoInvalido_seRechaza() {
        assertThatThrownBy(() -> imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                new byte[]{1, 2, 3, 4}
        )).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Formato de imagen no permitido");

        verify(imagenRepository, never()).save(any());
        verify(edificioRepository, never()).findById(any());
    }

    @Test
    void registrar_enElLimiteDeTamanio_seAcepta() {
        ReflectionTestUtils.setField(imagenService, "tamanioMaximo", 8L);
        edificio(Estado.ACTIVO);

        when(imagenRepository.save(any(Imagen.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Imagen resultado = imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                PNG
        );

        assertThat(resultado.getTamanioBytes()).isEqualTo(8L);
    }

    @Test
    void registrar_superaElTamanioMaximo_seRechaza() {
        ReflectionTestUtils.setField(imagenService, "tamanioMaximo", 7L);

        assertThatThrownBy(() -> imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                PNG
        )).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tamaño máximo");

        verify(imagenRepository, never()).save(any());
        verify(edificioRepository, never()).findById(any());
    }

    @Test
    void registrar_elementoInexistente_seRechaza() {
        when(edificioRepository.findById(EDIFICIO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                PNG
        )).isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("EDIFICIO");

        verify(imagenRepository, never()).save(any());
    }

    @Test
    void registrar_elementoInactivo_seRechaza() {
        edificio(Estado.INACTIVO);

        assertThatThrownBy(() -> imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                PNG
        )).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");

        verify(imagenRepository, never()).save(any());
    }

    @Test
    void registrar_sinContenido_seRechaza() {
        assertThatThrownBy(() -> imagenService.registrar(
                TipoElementoImagen.EDIFICIO,
                EDIFICIO_ID,
                "imagen.png",
                new byte[0]
        )).isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("vacía");

        verify(imagenRepository, never()).save(any());
    }
}
