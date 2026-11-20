package edu.eci.dosw.campusseguro.campus.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AlmacenamientoImagenLocalTest {

    private static final String RUTA = "imagenes/foto.png";
    private static final byte[] DATOS = {1, 2, 3, 4};

    @TempDir
    Path temporal;

    private AlmacenamientoImagenLocal almacenamiento;

    @BeforeEach
    void preparar() {
        almacenamiento = new AlmacenamientoImagenLocal(temporal.toString());
    }

    @Test
    void guardaYLeeElMismoContenido() throws IOException {
        almacenamiento.guardar(RUTA, DATOS);

        assertTrue(Files.isRegularFile(temporal.resolve(RUTA)));
        assertArrayEquals(DATOS, almacenamiento.leer(RUTA));
    }

    @Test
    void noSobrescribeUnArchivoExistente() {
        almacenamiento.guardar(RUTA, DATOS);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.guardar(RUTA, DATOS));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getStatusCode());
    }

    @Test
    void leerUnArchivoInexistenteResponde404() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.leer(RUTA));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void eliminaElArchivoYLuegoNoExiste() {
        almacenamiento.guardar(RUTA, DATOS);

        almacenamiento.eliminar(RUTA);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> almacenamiento.leer(RUTA));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void eliminarUnArchivoInexistenteNoFalla() {
        almacenamiento.eliminar(RUTA);
    }

    @Test
    void rechazaRutasInvalidas() {
        for (String ruta : new String[] {null, "", "   ", "../fuera.png", "imagenes/../../fuera.png", "/etc/passwd"}) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> almacenamiento.guardar(ruta, DATOS));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
    }
}
