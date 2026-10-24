package edu.eci.dosw.campusseguro.campus.ruta.controller;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaConsultadaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.service.ConsultaRutaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ConsultaRutaController.class)
class ConsultaRutaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaRutaService consultaService;

    private final UUID espacioId = uuid(10);
    private final UUID zonaId = uuid(11);

    private RutaConsultadaResponse conRuta() {
        return new RutaConsultadaResponse(espacioId, "Salón 101", true, zonaId,
                "Plazoleta", "Frente al edificio A", "Por la puerta norte", EstadoRuta.DISPONIBLE, null, true, null);
    }

    @Test
    void porEspacio_devuelveLaRutaRecomendada() throws Exception {
        when(consultaService.consultarPorEspacio(espacioId)).thenReturn(conRuta());

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", espacioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zonaSeguraNombre").value("Plazoleta"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void porEspacio_inexistenteDevuelve404() throws Exception {
        when(consultaService.consultarPorEspacio(espacioId))
                .thenThrow(new RecursoNoEncontradoException("El espacio no existe"));

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", espacioId))
                .andExpect(status().isNotFound());
    }

    @Test
    void porEspacio_conIdMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", "no-es-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'spaceId')]").exists());
    }

    @Test
    void porEdificio_devuelveUnaRutaPorEspacio() throws Exception {
        UUID edificioId = uuid(12);
        when(consultaService.consultarPorEdificio(edificioId)).thenReturn(List.of(conRuta()));

        mockMvc.perform(get("/api/v1/buildings/{buildingId}/evacuation-routes", edificioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].espacioNombre").value("Salón 101"));
    }

    @Test
    void porEdificio_sinEspaciosDevuelveListaVacia() throws Exception {
        UUID edificioId = uuid(13);
        when(consultaService.consultarPorEdificio(edificioId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/buildings/{buildingId}/evacuation-routes", edificioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
