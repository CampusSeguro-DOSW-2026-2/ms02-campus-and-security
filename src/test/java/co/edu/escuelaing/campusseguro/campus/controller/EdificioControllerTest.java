package co.edu.escuelaing.campusseguro.campus.controller;

import co.edu.escuelaing.campusseguro.campus.dto.EdificioRequest;
import co.edu.escuelaing.campusseguro.campus.dto.EdificioResponse;
import co.edu.escuelaing.campusseguro.campus.entity.Estado;
import co.edu.escuelaing.campusseguro.campus.exception.CodigoDuplicadoException;
import co.edu.escuelaing.campusseguro.campus.exception.EdificioNoEncontradoException;
import co.edu.escuelaing.campusseguro.campus.service.EdificioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(EdificioController.class)
class EdificioControllerTest {

    private static final String BASE = "/api/v1/buildings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EdificioService edificioService;

    private EdificioResponse respuesta(Estado estado) {
        return new EdificioResponse(1L, "ED-1", "Bloque H", "Descripción", estado);
    }

    private String json(String codigo, String nombre) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre
                + "\",\"descripcion\":\"Descripción\"}";
    }

    @Test
    void crear_devuelve201ConElEdificioActivo() throws Exception {
        when(edificioService.crear(any(EdificioRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "Bloque H")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ED-1"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_sinCodigoNiNombre_devuelve400IndicandoLosCampos() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").exists())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    void crear_conNombreMayorA100Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "a".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
    }

    @Test
    void crear_codigoDuplicado_devuelve409() throws Exception {
        when(edificioService.crear(any(EdificioRequest.class)))
                .thenThrow(new CodigoDuplicadoException("ED-1"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "Bloque H")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("codigo"));
    }

    @Test
    void modificar_devuelve200ConLosDatosActualizados() throws Exception {
        when(edificioService.modificar(eq(1L), any(EdificioRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put(BASE + "/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "Bloque H")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bloque H"));
    }

    @Test
    void modificar_noExiste_devuelve404() throws Exception {
        when(edificioService.modificar(eq(99L), any(EdificioRequest.class)))
                .thenThrow(new EdificioNoEncontradoException(99L));

        mockMvc.perform(put(BASE + "/{id}", 99L).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "Bloque H")))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactivar_devuelveElEdificioInactivo() throws Exception {
        when(edificioService.inactivar(1L)).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void inactivar_noExiste_devuelve404() throws Exception {
        when(edificioService.inactivar(99L)).thenThrow(new EdificioNoEncontradoException(99L));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveLosEdificios() throws Exception {
        when(edificioService.listar()).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ED-1"));
    }

    @Test
    void obtener_devuelveElEdificio() throws Exception {
        when(edificioService.obtener(1L)).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(get(BASE + "/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtener_noExiste_devuelve404() throws Exception {
        when(edificioService.obtener(99L)).thenThrow(new EdificioNoEncontradoException(99L));

        mockMvc.perform(get(BASE + "/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_conIdMalFormado_devuelve400() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "no-es-numero"))
                .andExpect(status().isBadRequest());
    }
}