
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
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
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
        return new EdificioResponse(
                uuid(1), "Bloque H", "Descripción", estado, uuid(10), false);
    }

    private String json(String nombre) {
        return "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Descripción\",\"campusId\":\"" + uuid(10) + "\"}";
    }

    private String jsonModificar(String nombre) {
        return "{\"nombre\":\"" + nombre
                + "\",\"descripcion\":\"Descripción\"}";
    }

    @Test
    void crear_devuelve201ConElEdificioActivo() throws Exception {
        when(edificioService.crear(any(EdificioRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Bloque H")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Bloque H"))
                .andExpect(jsonPath("$.campusId").value(uuid(10).toString()))
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_sinNombreNiCampus_devuelve400IndicandoLosCampos() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'campusId')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void crear_conNombreMayorA100Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("a".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void modificar_devuelve200ConLosDatosActualizados() throws Exception {
        when(edificioService.modificar(
                eq(uuid(1)), any(ModificarEdificioRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put(BASE + "/{id}", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Bloque H")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Bloque H"));
    }

    @Test
    void modificar_noExiste_devuelve404() throws Exception {
        when(edificioService.modificar(
                eq(uuid(99)), any(ModificarEdificioRequest.class)))
                .thenThrow(new EdificioNoEncontradoException(uuid(99)));

        mockMvc.perform(put(BASE + "/{id}", uuid(99))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Bloque H")))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactivar_devuelveElEdificioInactivo() throws Exception {
        when(edificioService.inactivar(uuid(1)))
                .thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void inactivar_conRutasActivas_devuelve422() throws Exception {
        when(edificioService.inactivar(uuid(1)))
                .thenThrow(new ReglaNegocioException("El edificio tiene rutas activas"));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
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
        when(edificioService.listar(false))
                .thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Bloque H"));
    }

    @Test
    void listar_conIncluirInactivos_pasaElFiltro() throws Exception {
        when(edificioService.listar(true))
                .thenReturn(List.of(respuesta(Estado.INACTIVO)));

        mockMvc.perform(get(BASE).param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void obtener_devuelveElEdificio() throws Exception {
        when(edificioService.obtener(uuid(1)))
                .thenReturn(respuesta(Estado.ACTIVO));

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

    @Test
    void reactivar_devuelveElEdificioActivo() throws Exception {
        when(edificioService.reactivar(uuid(1)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void reactivar_yaActivo_devuelve422() throws Exception {
        when(edificioService.reactivar(uuid(1)))
                .thenThrow(new ReglaNegocioException("El edificio ya está activo"));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void reactivar_noExiste_devuelve404() throws Exception {
        when(edificioService.reactivar(uuid(99)))
                .thenThrow(new EdificioNoEncontradoException(uuid(99)));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(99)))
                .andExpect(status().isNotFound());
    }
}
