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

import edu.eci.dosw.campusseguro.campus.dto.ModificarZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraRequest;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.ZonaCodigoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.ZonaSeguraNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.service.ZonaSeguraService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ZonaSeguraController.class)
class ZonaSeguraControllerTest {

    private static final String BASE = "/api/v1/safe-zones";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZonaSeguraService zonaSeguraService;

    private ZonaSeguraResponse respuesta(Estado estado) {
        return new ZonaSeguraResponse(uuid(1), "MP-H-01", "Punto H",
                "Frente al bloque H", "Zona abierta", 200, estado);
    }

    private String json(String codigo, String nombre, int capacidad) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre
                + "\",\"ubicacion\":\"Frente al bloque H\""
                + ",\"descripcion\":\"Zona abierta\""
                + ",\"capacidad\":" + capacidad + "}";
    }

    private String jsonModificar(String nombre, int capacidad) {
        return "{\"nombre\":\"" + nombre
                + "\",\"ubicacion\":\"Frente al bloque H\""
                + ",\"descripcion\":\"Zona abierta\""
                + ",\"capacidad\":" + capacidad + "}";
    }

    // ---------- crear ----------

    @Test
    void crear_devuelve201ConLaZonaActiva() throws Exception {
        when(zonaSeguraService.crear(any(ZonaSeguraRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("MP-H-01", "Punto H", 200)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MP-H-01"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_sinDatosObligatorios_devuelve400IndicandoLosCampos() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'codigo')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'capacidad')]").exists());
    }

    @Test
    void crear_conCapacidadCero_devuelve400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("MP-H-01", "Punto H", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'capacidad')]").exists());
    }

    @Test
    void crear_codigoDuplicado_devuelve409() throws Exception {
        when(zonaSeguraService.crear(any(ZonaSeguraRequest.class)))
                .thenThrow(new ZonaCodigoDuplicadoException("MP-H-01"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("MP-H-01", "Punto H", 200)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores[0].campo").value("codigo"));
    }

    // ---------- modificar ----------

    @Test
    void modificar_devuelve200ConLosDatosActualizados() throws Exception {
        when(zonaSeguraService.modificar(
                eq(uuid(1)), any(ModificarZonaSeguraRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put(BASE + "/{id}", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Punto H", 200)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Punto H"));
    }

    @Test
    void modificar_noExiste_devuelve404() throws Exception {
        when(zonaSeguraService.modificar(
                eq(uuid(99)), any(ModificarZonaSeguraRequest.class)))
                .thenThrow(new ZonaSeguraNoEncontradaException(uuid(99)));

        mockMvc.perform(put(BASE + "/{id}", uuid(99))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Punto H", 200)))
                .andExpect(status().isNotFound());
    }

    @Test
    void modificar_conCapacidadCero_devuelve400() throws Exception {
        mockMvc.perform(put(BASE + "/{id}", uuid(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonModificar("Punto H", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'capacidad')]").exists());
    }

    // ---------- inactivar ----------

    @Test
    void inactivar_devuelveLaZonaInactiva() throws Exception {
        when(zonaSeguraService.inactivar(uuid(1)))
                .thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void inactivar_conRutasActivas_devuelve422() throws Exception {
        when(zonaSeguraService.inactivar(uuid(1)))
                .thenThrow(new ReglaNegocioException(
                        "La zona segura es destino de rutas activas"));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void inactivar_noExiste_devuelve404() throws Exception {
        when(zonaSeguraService.inactivar(uuid(99)))
                .thenThrow(new ZonaSeguraNoEncontradaException(uuid(99)));

        mockMvc.perform(patch(BASE + "/{id}/deactivate", uuid(99)))
                .andExpect(status().isNotFound());
    }

    // ---------- reactivar ----------

    @Test
    void reactivar_devuelveLaZonaActiva() throws Exception {
        when(zonaSeguraService.reactivar(uuid(1)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void reactivar_yaActiva_devuelve422() throws Exception {
        when(zonaSeguraService.reactivar(uuid(1)))
                .thenThrow(new ReglaNegocioException(
                        "La zona segura ya está activa"));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void reactivar_noExiste_devuelve404() throws Exception {
        when(zonaSeguraService.reactivar(uuid(99)))
                .thenThrow(new ZonaSeguraNoEncontradaException(uuid(99)));

        mockMvc.perform(patch(BASE + "/{id}/reactivate", uuid(99)))
                .andExpect(status().isNotFound());
    }

    // ---------- consultas ----------

    @Test
    void listar_porDefectoPideSoloLasActivas() throws Exception {
        when(zonaSeguraService.listar(false))
                .thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MP-H-01"));
    }

    @Test
    void listar_conIncluirInactivos_pasaElFiltro() throws Exception {
        when(zonaSeguraService.listar(true))
                .thenReturn(List.of(respuesta(Estado.INACTIVO)));

        mockMvc.perform(get(BASE).param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void obtener_devuelveLaZona() throws Exception {
        when(zonaSeguraService.obtener(uuid(1)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(get(BASE + "/{id}", uuid(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid(1).toString()));
    }

    @Test
    void obtener_noExiste_devuelve404() throws Exception {
        when(zonaSeguraService.obtener(uuid(99)))
                .thenThrow(new ZonaSeguraNoEncontradaException(uuid(99)));

        mockMvc.perform(get(BASE + "/{id}", uuid(99)))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_conIdMalFormado_devuelve400() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "no-es-uuid"))
                .andExpect(status().isBadRequest());
    }
}