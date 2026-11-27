package edu.eci.dosw.campusseguro.campus.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AlmacenamientoImagenAzureBlobTest {

    private static final String RUTA = "imagenes/foto.png";
    private static final byte[] DATOS = {1, 2, 3, 4};

    private BlobClient blob;
    private AlmacenamientoImagenAzureBlob almacenamiento;

    @BeforeEach
    void preparar() {
        BlobContainerClient contenedor = mock(BlobContainerClient.class);
        blob = mock(BlobClient.class);
        when(contenedor.getBlobClient(RUTA)).thenReturn(blob);
        almacenamiento = new AlmacenamientoImagenAzureBlob(contenedor);
    }

    @Test
    void guardaElContenidoSinSobrescribir() {
        almacenamiento.guardar(RUTA, DATOS);

        ArgumentCaptor<BinaryData> captor = ArgumentCaptor.forClass(BinaryData.class);
        verify(blob).upload(captor.capture(), eq(false));
        assertArrayEquals(DATOS, captor.getValue().toBytes());
    }

    @Test
    void guardarConErrorDeAzureResponde500() {
        BlobStorageException falla = mock(BlobStorageException.class);
        doThrow(falla).when(blob).upload(any(BinaryData.class), eq(false));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.guardar(RUTA, DATOS));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
    }

    @Test
    void leeElContenidoDelBlob() {
        when(blob.downloadContent()).thenReturn(BinaryData.fromBytes(DATOS));

        assertArrayEquals(DATOS, almacenamiento.leer(RUTA));
    }

    @Test
    void leerUnBlobInexistenteResponde404() {
        BlobStorageException noExiste = mock(BlobStorageException.class);
        when(noExiste.getStatusCode()).thenReturn(404);
        when(blob.downloadContent()).thenThrow(noExiste);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.leer(RUTA));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void leerConOtroErrorDeAzureResponde500() {
        BlobStorageException falla = mock(BlobStorageException.class);
        when(falla.getStatusCode()).thenReturn(503);
        when(blob.downloadContent()).thenThrow(falla);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.leer(RUTA));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
    }

    @Test
    void eliminaElBlob() {
        almacenamiento.eliminar(RUTA);

        verify(blob).deleteIfExists();
    }

    @Test
    void eliminarConErrorDeAzureResponde500() {
        BlobStorageException falla = mock(BlobStorageException.class);
        when(blob.deleteIfExists()).thenThrow(falla);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.eliminar(RUTA));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
    }

    @Test
    void rechazaRutasInvalidas() {
        for (String ruta : new String[] {null, "", "   ", "../fuera.png", "/absoluta.png", "a\\b.png"}) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> almacenamiento.guardar(ruta, DATOS));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
    }
}
