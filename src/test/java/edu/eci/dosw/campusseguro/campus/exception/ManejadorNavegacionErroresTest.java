package edu.eci.dosw.campusseguro.campus.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.eci.dosw.campusseguro.campus.ruta.exception.NavegacionNoDisponibleException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaNoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ManejadorNavegacionErroresTest {

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/sin-ruta")
        String sinRuta() {
            throw new RutaNoEncontradaException();
        }

        @GetMapping("/prueba/no-disponible")
        String noDisponible() {
            throw new NavegacionNoDisponibleException("detalle interno del proveedor");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorGlobalErrores())
                .build();
    }

    @Test
    void rutaNoEncontrada_devuelve422() throws Exception {
        mockMvc.perform(get("/prueba/sin-ruta"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RUTA_NO_ENCONTRADA"));
    }

    @Test
    void navegacionNoDisponible_devuelve503SinExponerElDetalle() throws Exception {
        mockMvc.perform(get("/prueba/no-disponible"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("NAVEGACION_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.mensaje").value(
                        "El servicio de navegación no está disponible. Intente de nuevo más tarde."));
    }
}
