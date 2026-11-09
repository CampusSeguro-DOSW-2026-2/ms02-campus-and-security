package edu.eci.dosw.campusseguro.campus.service;

import java.util.UUID;
import java.util.List;

import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Imagen;
import edu.eci.dosw.campusseguro.campus.entity.TipoElementoImagen;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.EspacioRepository;
import edu.eci.dosw.campusseguro.campus.repository.ImagenRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.repository.ZonaSeguraRepository;
import edu.eci.dosw.campusseguro.campus.storage.AlmacenamientoImagenLocal;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImagenService {

    private static final long TAMANIO_MAXIMO_PREDETERMINADO = 5 * 1024 * 1024;

    private final ImagenRepository imagenRepository;
    private final EdificioRepository edificioRepository;
    private final EspacioRepository espacioRepository;
    private final SalidaRepository salidaRepository;
    private final ZonaSeguraRepository zonaSeguraRepository;
    private final AlmacenamientoImagenLocal almacenamiento;

    @Value("${campusseguro.imagen.max-size-bytes:5242880}")
    private long tamanioMaximo;

    @Transactional(readOnly = true)
    public List<Imagen> listar(TipoElementoImagen tipoElemento, UUID elementoId) {
        return imagenRepository.findAllByTipoElementoAndElementoId(tipoElemento, elementoId);
    }

    public Imagen registrar(
            TipoElementoImagen tipoElemento,
            UUID elementoId,
            String nombreOriginal,
            byte[] contenido) {

        validarDatos(tipoElemento, elementoId, nombreOriginal, contenido);
        validarTamanio(contenido.length);

        FormatoImagen formato = detectarFormato(contenido);
        validarElementoActivo(tipoElemento, elementoId);

        UUID nombreSeguro = UUID.randomUUID();

        Imagen imagen = new Imagen();
        imagen.setTipoElemento(tipoElemento);
        imagen.setElementoId(elementoId);
        imagen.setNombreOriginal(nombreOriginal.trim());
        imagen.setTipoContenido(formato.tipoContenido());
        imagen.setTamanioBytes((long) contenido.length);
        imagen.setRutaArchivo("imagenes/" + nombreSeguro + formato.extension());

        almacenamiento.guardar(imagen.getRutaArchivo(), contenido);

        try {
            return imagenRepository.saveAndFlush(imagen);
        } catch (RuntimeException e) {
            try {
                almacenamiento.eliminar(imagen.getRutaArchivo());
            } catch (RuntimeException errorLimpieza) {
                e.addSuppressed(errorLimpieza);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Imagen obtener(UUID id) {
        return imagenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una imagen con id " + id));
    }

    @Transactional(readOnly = true)
    public byte[] obtenerContenido(UUID id) {
        Imagen imagen = obtener(id);
        return almacenamiento.leer(imagen.getRutaArchivo());
    }

    @Transactional
    public void eliminar(UUID id) {
        Imagen imagen = obtener(id);

        imagenRepository.delete(imagen);
        imagenRepository.flush();

        almacenamiento.eliminar(imagen.getRutaArchivo());
    }

    private void validarDatos(
            TipoElementoImagen tipoElemento,
            UUID elementoId,
            String nombreOriginal,
            byte[] contenido) {

        if (tipoElemento == null) {
            throw new ReglaNegocioException("El tipo de elemento es obligatorio.");
        }

        if (elementoId == null) {
            throw new ReglaNegocioException("El identificador del elemento es obligatorio.");
        }

        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new ReglaNegocioException("El nombre original de la imagen es obligatorio.");
        }

        if (nombreOriginal.trim().length() > 255) {
            throw new ReglaNegocioException("El nombre original no puede superar los 255 caracteres.");
        }

        if (contenido == null || contenido.length == 0) {
            throw new ReglaNegocioException("La imagen no puede estar vacía.");
        }
    }

    private void validarTamanio(long tamanio) {
        long limite = tamanioMaximo > 0
                ? tamanioMaximo
                : TAMANIO_MAXIMO_PREDETERMINADO;

        if (tamanio > limite) {
            throw new ReglaNegocioException(
                    "La imagen supera el tamaño máximo permitido de " + limite + " bytes.");
        }
    }

    private FormatoImagen detectarFormato(byte[] contenido) {
        if (esJpeg(contenido)) {
            return new FormatoImagen("image/jpeg", ".jpg");
        }

        if (esPng(contenido)) {
            return new FormatoImagen("image/png", ".png");
        }

        if (esWebp(contenido)) {
            return new FormatoImagen("image/webp", ".webp");
        }

        throw new ReglaNegocioException(
                "Formato de imagen no permitido. Use JPEG, PNG o WEBP.");
    }

    private boolean esJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean esPng(byte[] bytes) {
        byte[] firma = {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (bytes.length < firma.length) {
            return false;
        }

        for (int i = 0; i < firma.length; i++) {
            if (bytes[i] != firma[i]) {
                return false;
            }
        }

        return true;
    }

    private boolean esWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }

    private void validarElementoActivo(
            TipoElementoImagen tipoElemento,
            UUID elementoId) {

        Estado estado = switch (tipoElemento) {
            case EDIFICIO -> edificioRepository.findById(elementoId)
                    .map(e -> e.getEstado())
                    .orElseThrow(() -> elementoNoEncontrado(tipoElemento, elementoId));

            case ESPACIO -> espacioRepository.findById(elementoId)
                    .map(e -> e.getEstado())
                    .orElseThrow(() -> elementoNoEncontrado(tipoElemento, elementoId));

            case SALIDA -> salidaRepository.findById(elementoId)
                    .map(e -> e.getEstado())
                    .orElseThrow(() -> elementoNoEncontrado(tipoElemento, elementoId));

            case ZONA_SEGURA -> zonaSeguraRepository.findById(elementoId)
                    .map(e -> e.getEstado())
                    .orElseThrow(() -> elementoNoEncontrado(tipoElemento, elementoId));
        };

        if (estado != Estado.ACTIVO) {
            throw new ReglaNegocioException(
                    "No se pueden asociar imágenes a un elemento inactivo.");
        }
    }

    private RecursoNoEncontradoException elementoNoEncontrado(
            TipoElementoImagen tipoElemento,
            UUID elementoId) {

        return new RecursoNoEncontradoException(
                "No existe un elemento de tipo " + tipoElemento + " con id " + elementoId);
    }

    private record FormatoImagen(String tipoContenido, String extension) {
    }
}
