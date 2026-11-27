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

import java.util.List;

import edu.eci.dosw.campusseguro.campus.dto.CampusRequest;
import edu.eci.dosw.campusseguro.campus.dto.CampusResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CampusNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.service.CampusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CampusController.class)
class CampusControllerTest {

    private static final String BASE = "/api/v1/campuses";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampusService campusService;

    private CampusResponse respuesta(Estado estado) {
        return new CampusResponse(uuid(1), "Campus principal", "Descripción", estado);
    }

    private String json(String nombre) {
        return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Descripción\"}";
    }

    @Test
    void crear_devuelve201ConElCampusActivo() throws Exception {
        when(campusService.crear(any(CampusRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json("Campus principal")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(uuid(1).toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_sinNombre_devuelve400IndicandoElCampo() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void crear_conNombreMayorA100Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json("a".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void modificar_devuelve200() throws Exception {
        when(campusService.modificar(eq(uuid(1)), any(CampusRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put(BASE + "/{id}", uuid(1)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Campus principal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Campus principal"));
    }

    @Test
    void modificar_noExiste_devuelve404() throws Exception {
        when(campusService.modificar(eq(uuid(99)), any(CampusRequest.class)))
                .thenThrow(new CampusNoEncontradoException(uuid(99)));

        mockMvc.perform(put(BASE + "/{id}", uuid(99)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Campus\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactivar_devuelveElCampusInactivo() throws Exception {
        when(campusService.inactivar(uuid(1))).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void inactivar_conHijosActivos_devuelve422() throws Exception {
        when(campusService.inactivar(uuid(1))).thenThrow(new ReglaNegocioException("El campus tiene edificios activos"));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void reactivar_devuelveElCampusActivo() throws Exception {
        when(campusService.reactivar(uuid(1))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void listar_devuelveLosCampus() throws Exception {
        when(campusService.listar(false)).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Campus principal"));
    }

    @Test
    void obtener_devuelveElCampus() throws Exception {
        when(campusService.obtener(uuid(1))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(get(BASE + "/{id}", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid(1).toString()));
    }
}
