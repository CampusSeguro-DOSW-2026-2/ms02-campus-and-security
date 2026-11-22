package edu.eci.dosw.campusseguro.campus.ruta.controller;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Crear y actualizar rutas a mano se quitaron (RF-MS02-05: solo quedan las rutas que se generan
// solas). Lo que queda en este controlador es consultar, listar y cambiar el estado.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RutaController.class)
class RutaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RutaService rutaService;

    private final UUID rutaId = uuid(10);
    private final UUID espacioId = uuid(11);
    private final UUID zonaId = uuid(12);

    private RutaResponse respuesta(EstadoRuta estado) {
        String motivo = estado == EstadoRuta.DISPONIBLE
                ? null
                : "Obra en la escalera norte";

        return new RutaResponse(
                rutaId,
                espacioId,
                zonaId,
                "Por la puerta norte",
                estado,
                motivo,
                Instant.now());
    }

    @Test
    void cambiarEstado_devuelveLaRutaActualizada() throws Exception {
        when(rutaService.cambiarEstado(rutaId, EstadoRuta.BLOQUEADA, "Obra en la escalera norte"))
                .thenReturn(respuesta(EstadoRuta.BLOQUEADA));

        mockMvc.perform(patch("/api/v1/evacuation-routes/{id}/status", rutaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADA\",\"motivo\":\"Obra en la escalera norte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.motivo").value("Obra en la escalera norte"));
    }

    @Test
    void cambiarEstado_sinEstadoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/evacuation-routes/{id}/status", rutaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'estado')]").exists());
    }

    @Test
    void obtener_rutaInexistenteDevuelve404() throws Exception {
        when(rutaService.obtener(rutaId)).thenThrow(new RecursoNoEncontradoException("La ruta no existe"));

        mockMvc.perform(get("/api/v1/evacuation-routes/{id}", rutaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_conIdMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/evacuation-routes/{id}", "no-es-numero"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_devuelveLasRutas() throws Exception {
        when(rutaService.listar()).thenReturn(List.of(respuesta(EstadoRuta.DISPONIBLE)));

        mockMvc.perform(get("/api/v1/evacuation-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rutaId.toString()));
    }
}
