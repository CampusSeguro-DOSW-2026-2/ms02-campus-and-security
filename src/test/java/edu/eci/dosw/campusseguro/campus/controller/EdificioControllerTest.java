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

import edu.eci.dosw.campusseguro.campus.dto.EdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEdificioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EdificioResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.CodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.service.EdificioService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(EdificioController.class)
class EdificioControllerTest {

    private static final String BASE = "/api/v1/buildings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EdificioService edificioService;

    private EdificioResponse respuesta(Estado estado) {
        return new EdificioResponse(uuid(1), "ED-1", "Bloque H", "Descripción", estado);
    }

    private String json(String codigo, String nombre) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre
                + "\",\"descripcion\":\"Descripción\"}";
    }

    private String jsonModificar(String nombre) {
        return "{\"nombre\":\"" + nombre
                + "\",\"descripcion\":\"Descripción\"}";
    }

    @Test
    void crear_devuelve201ConElEdificioActivo() throws Exception {
        when(edificioService.crear(any(EdificioRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

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
                .andExpect(jsonPath("$.errores[?(@.campo == 'codigo')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void crear_conNombreMayorA100Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "a".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void crear_codigoDuplicado_devuelve409() throws Exception {
        when(edificioService.crear(any(EdificioRequest.class)))
                .thenThrow(new CodigoDuplicadoException("ED-1"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(json("ED-1", "Bloque H")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores[0].campo").value("codigo"));
    }

    @Test
    void modificar_devuelve200ConLosDatosActualizados() throws Exception {
        when(edificioService.modificar(eq(uuid(1)), any(ModificarEdificioRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put(BASE + "/{id}", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Bloque H")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bloque H"));
    }

    @Test
    void modificar_noExiste_devuelve404() throws Exception {
        when(edificioService.modificar(eq(uuid(99)), any(ModificarEdificioRequest.class)))
                .thenThrow(new EdificioNoEncontradoException(uuid(99)));

        mockMvc.perform(put(BASE + "/{id}", uuid(99))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Bloque H")))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactivar_devuelveElEdificioInactivo() throws Exception {
        when(edificioService.inactivar(uuid(1))).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void inactivar_noExiste_devuelve404() throws Exception {
        when(edificioService.inactivar(uuid(99)))
                .thenThrow(new EdificioNoEncontradoException(uuid(99)));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(99)))
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
        when(edificioService.obtener(uuid(1))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(get(BASE + "/{id}", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid(1).toString()));
    }

    @Test
    void obtener_noExiste_devuelve404() throws Exception {
        when(edificioService.obtener(uuid(99)))
                .thenThrow(new EdificioNoEncontradoException(uuid(99)));

        mockMvc.perform(get(BASE + "/{id}", uuid(99)))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_conIdMalFormado_devuelve400() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "no-es-numero"))
                .andExpect(status().isBadRequest());
    }
}