package edu.eci.dosw.campusseguro.campus.controller;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaAbiertaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoZonaAbierta;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaAbiertaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.service.ZonaAbiertaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ZonaAbiertaController.class)
class ZonaAbiertaControllerTest {

    private static final String JSON_ZONA =
            "{\"nombre\":\"Plazoleta\",\"tipo\":\"PLAZOLETA\",\"latitud\":4.782,\"longitud\":-74.042}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZonaAbiertaService zonaAbiertaService;

    private ZonaAbiertaResponse respuesta(Estado estado) {
        return new ZonaAbiertaResponse(uuid(2), uuid(1), "Plazoleta", TipoZonaAbierta.PLAZOLETA,
                new BigDecimal("4.782000"), new BigDecimal("-74.042000"), estado);
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(zonaAbiertaService.crear(eq(uuid(1)), any(ZonaAbiertaRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post("/api/v1/campuses/{id}/open-zones", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_ZONA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(uuid(2).toString()))
                .andExpect(jsonPath("$.tipo").value("PLAZOLETA"));
    }

    @Test
    void crear_sinNombreNiTipo_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/campuses/{id}/open-zones", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'tipo')]").exists());
    }

    @Test
    void crear_conLatitudFueraDeRango_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/campuses/{id}/open-zones", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"P\",\"tipo\":\"OTRA\",\"latitud\":95,\"longitud\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'latitud')]").exists());
    }

    @Test
    void crear_campusInexistente_devuelve404() throws Exception {
        when(zonaAbiertaService.crear(eq(uuid(99)), any(ZonaAbiertaRequest.class)))
                .thenThrow(new CampusNoEncontradoException(uuid(99)));

        mockMvc.perform(post("/api/v1/campuses/{id}/open-zones", uuid(99))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_ZONA))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorCampus_devuelveLasZonas() throws Exception {
        when(zonaAbiertaService.listarPorCampus(uuid(1), false)).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/campuses/{id}/open-zones", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Plazoleta"));
    }

    @Test
    void obtener_noExiste_devuelve404() throws Exception {
        when(zonaAbiertaService.obtener(uuid(99))).thenThrow(new ZonaAbiertaNoEncontradaException(uuid(99)));

        mockMvc.perform(get("/api/v1/open-zones/{id}", uuid(99)))
                .andExpect(status().isNotFound());
    }

    @Test
    void modificar_devuelve200() throws Exception {
        when(zonaAbiertaService.modificar(eq(uuid(2)), any(ZonaAbiertaRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put("/api/v1/open-zones/{id}", uuid(2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Plazoleta\",\"tipo\":\"PLAZOLETA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Plazoleta"));
    }

    @Test
    void inactivarYReactivar_devuelvenLaZona() throws Exception {
        when(zonaAbiertaService.inactivar(uuid(2))).thenReturn(respuesta(Estado.INACTIVO));
        when(zonaAbiertaService.reactivar(uuid(2))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(patch("/api/v1/open-zones/{id}/deactivate", uuid(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
        mockMvc.perform(patch("/api/v1/open-zones/{id}/reactivate", uuid(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }
}
