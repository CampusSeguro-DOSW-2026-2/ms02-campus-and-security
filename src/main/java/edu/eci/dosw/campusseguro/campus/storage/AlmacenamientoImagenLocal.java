package edu.eci.dosw.campusseguro.campus.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AlmacenamientoImagenLocal {

    private final Path directorioBase;

    public AlmacenamientoImagenLocal(
            @Value("${campusseguro.imagen.storage-dir:./data}") String directorioBase) {
        this.directorioBase = Path.of(directorioBase)
                .toAbsolutePath()
                .normalize();
    }

    public void guardar(String rutaRelativa, byte[] contenido) {
        Path destino = resolver(rutaRelativa);

        try {
            Files.createDirectories(destino.getParent());
            Files.write(
                    destino,
                    contenido,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible almacenar la imagen."
            );
        }
    }

    public byte[] leer(String rutaRelativa) {
        Path archivo = resolver(rutaRelativa);

        try {
            if (!Files.isRegularFile(archivo)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El archivo de imagen no existe."
                );
            }

            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible leer la imagen."
            );
        }
    }

    public void eliminar(String rutaRelativa) {
        Path archivo = resolver(rutaRelativa);

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible eliminar la imagen."
            );
        }
    }

    private Path resolver(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ruta de la imagen es inválida."
            );
        }

        Path relativa = Path.of(rutaRelativa);

        if (relativa.isAbsolute()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ruta de la imagen es inválida."
            );
        }

        Path destino = directorioBase.resolve(relativa).normalize();

        if (!destino.startsWith(directorioBase)
                || destino.equals(directorioBase)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ruta de la imagen está fuera del almacenamiento permitido."
            );
        }

        return destino;
    }
}
