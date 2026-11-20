package edu.eci.dosw.campusseguro.campus.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guarda las fotografías en un contenedor de Azure Blob Storage (uno por entorno). Se activa con
 * {@code campusseguro.imagen.storage-type=azure}. El nombre del blob es la misma ruta relativa que usa el disco.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "campusseguro.imagen", name = "storage-type", havingValue = "azure")
public class AlmacenamientoImagenAzureBlob implements AlmacenamientoImagen {

    private final BlobContainerClient contenedor;

    @Autowired
    public AlmacenamientoImagenAzureBlob(
            @Value("${campusseguro.imagen.azure.connection-string:}") String cadenaConexion,
            @Value("${campusseguro.imagen.azure.container-name:imagenes}") String nombreContenedor) {
        this(crearContenedor(cadenaConexion, nombreContenedor));
    }

    AlmacenamientoImagenAzureBlob(BlobContainerClient contenedor) {
        this.contenedor = contenedor;
    }

    private static BlobContainerClient crearContenedor(String cadenaConexion, String nombreContenedor) {
        if (cadenaConexion == null || cadenaConexion.isBlank()) {
            throw new IllegalStateException(
                    "Falta AZURE_STORAGE_CONNECTION_STRING para usar Azure Blob Storage.");
        }

        BlobContainerClient cliente = new BlobServiceClientBuilder()
                .connectionString(cadenaConexion)
                .buildClient()
                .getBlobContainerClient(nombreContenedor);
        cliente.createIfNotExists();
        return cliente;
    }

    @Override
    public void guardar(String rutaRelativa, byte[] contenido) {
        BlobClient blob = blob(rutaRelativa);

        try {
            // overwrite = false: igual que CREATE_NEW en disco, nunca pisa una foto existente
            blob.upload(BinaryData.fromBytes(contenido), false);
        } catch (RuntimeException e) {
            log.error("No fue posible almacenar la imagen {} en Azure Blob Storage", rutaRelativa, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible almacenar la imagen.",
                    e
            );
        }
    }

    @Override
    public byte[] leer(String rutaRelativa) {
        BlobClient blob = blob(rutaRelativa);

        try {
            return blob.downloadContent().toBytes();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El archivo de imagen no existe."
                );
            }
            log.error("No fue posible leer la imagen {} de Azure Blob Storage", rutaRelativa, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible leer la imagen.",
                    e
            );
        } catch (RuntimeException e) {
            log.error("No fue posible leer la imagen {} de Azure Blob Storage", rutaRelativa, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible leer la imagen.",
                    e
            );
        }
    }

    @Override
    public void eliminar(String rutaRelativa) {
        BlobClient blob = blob(rutaRelativa);

        try {
            blob.deleteIfExists();
        } catch (RuntimeException e) {
            log.error("No fue posible eliminar la imagen {} de Azure Blob Storage", rutaRelativa, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible eliminar la imagen.",
                    e
            );
        }
    }

    private BlobClient blob(String rutaRelativa) {
        if (rutaRelativa == null
                || rutaRelativa.isBlank()
                || rutaRelativa.startsWith("/")
                || rutaRelativa.contains("\\")
                || rutaRelativa.contains("..")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ruta de la imagen es inválida."
            );
        }

        return contenedor.getBlobClient(rutaRelativa);
    }
}
