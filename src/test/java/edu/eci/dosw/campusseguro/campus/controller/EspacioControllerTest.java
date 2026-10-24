package edu.eci.dosw.campusseguro.campus.controller;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.EspacioRequest;
import edu.eci.dosw.campusseguro.campus.dto.EspacioResponse;
import edu.eci.dosw.campusseguro.campus.dto.ModificarEspacioRequest;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.exception.EspacioDuplicadoException;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.service.EspacioService;
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
@WebMvcTest(EspacioController.class)
class EspacioControllerTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID ESPACIO = uuid(10);
    private static final String ESPACIO_JSON = "{\"codigo\":\"G-101\",\"nombre\":\"Salón 101\",\"tipo\":\"SALON\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspacioService espacioService;

    private EspacioResponse respuesta(Estado estado) {
        return new EspacioResponse(ESPACIO, EDIFICIO, PISO, "G-101", "Salón 101", TipoEspacio.SALON, estado);
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(espacioService.crear(eq(PISO), any(EspacioRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post("/api/v1/floors/{id}/spaces", PISO).contentType(MediaType.APPLICATION_JSON)
                        .content(ESPACIO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("G-101"))
                .andExpect(jsonPath("$.pisoId").value(PISO.toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_sinCodigoNiNombreNiTipo_devuelve400ConLosCampos() throws Exception {
        mockMvc.perform(post("/api/v1/floors/{id}/spaces", PISO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'codigo')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'tipo')]").exists());
    }

    @Test
    void crear_conTipoInexistente_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/floors/{id}/spaces", PISO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"G-101\",\"nombre\":\"Bodega\",\"tipo\":\"BODEGA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void crear_conCodigoRepetido_devuelve409ConElCampo() throws Exception {
        when(espacioService.crear(eq(PISO), any(EspacioRequest.class)))
                .thenThrow(new EspacioDuplicadoException("G-101"));

        mockMvc.perform(post("/api/v1/floors/{id}/spaces", PISO).contentType(MediaType.APPLICATION_JSON)
                        .content(ESPACIO_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores[0].campo").value("codigo"));
    }

    @Test
    void crear_enPisoInactivo_devuelve422() throws Exception {
        when(espacioService.crear(eq(PISO), any(EspacioRequest.class)))
                .thenThrow(new ReglaNegocioException("No se pueden registrar espacios en un piso inactivo"));

        mockMvc.perform(post("/api/v1/floors/{id}/spaces", PISO).contentType(MediaType.APPLICATION_JSON)
                        .content(ESPACIO_JSON))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void listarPorPiso_devuelveLosEspacios() throws Exception {
        when(espacioService.listarPorPiso(PISO)).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/floors/{id}/spaces", PISO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("G-101"));
    }

    @Test
    void listarPorEdificio_devuelveLosEspacios() throws Exception {
        when(espacioService.listarPorEdificio(EDIFICIO)).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/buildings/{id}/spaces", EDIFICIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].edificioId").value(EDIFICIO.toString()));
    }

    @Test
    void obtener_espacioInexistente_devuelve404() throws Exception {
        when(espacioService.obtener(ESPACIO)).thenThrow(new EspacioNoEncontradoException(ESPACIO));

        mockMvc.perform(get("/api/v1/spaces/{id}", ESPACIO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void modificar_devuelveElEspacioActualizado() throws Exception {
        when(espacioService.modificar(eq(ESPACIO), any(ModificarEspacioRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put("/api/v1/spaces/{id}", ESPACIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Salón 101\",\"tipo\":\"SALON\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ESPACIO.toString()));
    }

    @Test
    void inactivar_conRutaActiva_devuelve422() throws Exception {
        when(espacioService.inactivar(ESPACIO))
                .thenThrow(new ReglaNegocioException("El espacio es origen de rutas activas"));

        mockMvc.perform(patch("/api/v1/spaces/{id}/deactivate", ESPACIO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El espacio es origen de rutas activas"));
    }

    @Test
    void inactivar_devuelveElEspacioInactivo() throws Exception {
        when(espacioService.inactivar(ESPACIO)).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch("/api/v1/spaces/{id}/deactivate", ESPACIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void reactivar_devuelveElEspacioActivo() throws Exception {
        when(espacioService.reactivar(ESPACIO)).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(patch("/api/v1/spaces/{id}/reactivate", ESPACIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void reactivar_conPisoInactivo_devuelve422() throws Exception {
        when(espacioService.reactivar(ESPACIO))
                .thenThrow(new ReglaNegocioException("El piso o el edificio está inactivo: debe reactivarse primero"));

        mockMvc.perform(patch("/api/v1/spaces/{id}/reactivate", ESPACIO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }
}
