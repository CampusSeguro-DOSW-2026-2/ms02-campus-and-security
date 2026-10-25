package edu.eci.dosw.campusseguro.campus.controller;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.PisoRequest;
import edu.eci.dosw.campusseguro.campus.dto.PisoResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.EdificioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.exception.PisoDuplicadoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.service.PisoService;
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
@WebMvcTest(PisoController.class)
class PisoControllerTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PisoService pisoService;

    private PisoResponse respuesta(Estado estado) {
        return new PisoResponse(PISO, EDIFICIO, -1, "Sótano 1", estado);
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(pisoService.crear(eq(EDIFICIO), any(PisoRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post("/api/v1/buildings/{id}/floors", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":-1,\"nombre\":\"Sótano 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(-1))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_sinNumero_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/floors", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sótano 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'numero')]").exists());
    }

    @Test
    void crear_conNombreMayorA100Caracteres_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/floors", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":1,\"nombre\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void crear_enEdificioInexistente_devuelve404() throws Exception {
        when(pisoService.crear(eq(EDIFICIO), any(PisoRequest.class)))
                .thenThrow(new EdificioNoEncontradoException(EDIFICIO));

        mockMvc.perform(post("/api/v1/buildings/{id}/floors", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void crear_conNumeroRepetido_devuelve409ConElCampo() throws Exception {
        when(pisoService.crear(eq(EDIFICIO), any(PisoRequest.class))).thenThrow(new PisoDuplicadoException(1));

        mockMvc.perform(post("/api/v1/buildings/{id}/floors", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores[0].campo").value("numero"));
    }

    @Test
    void crear_enEdificioInactivo_devuelve422() throws Exception {
        when(pisoService.crear(eq(EDIFICIO), any(PisoRequest.class)))
                .thenThrow(new ReglaNegocioException("No se pueden registrar pisos en un edificio inactivo"));

        mockMvc.perform(post("/api/v1/buildings/{id}/floors", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":1}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void listarPorEdificio_devuelveLosPisos() throws Exception {
        when(pisoService.listarPorEdificio(EDIFICIO, false)).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/buildings/{id}/floors", EDIFICIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Sótano 1"));
    }

    @Test
    void obtener_conIdInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/floors/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("id"));
    }

    @Test
    void modificar_devuelveElPisoActualizado() throws Exception {
        when(pisoService.modificar(eq(PISO), any(PisoRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put("/api/v1/floors/{id}", PISO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":-1,\"nombre\":\"Sótano 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PISO.toString()));
    }

    @Test
    void inactivar_conEspaciosActivos_devuelve422() throws Exception {
        when(pisoService.inactivar(PISO))
                .thenThrow(new ReglaNegocioException("El piso tiene espacios activos; deben inactivarse antes"));

        mockMvc.perform(patch("/api/v1/floors/{id}/deactivate", PISO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El piso tiene espacios activos; deben inactivarse antes"));
    }

    @Test
    void inactivar_devuelveElPisoInactivo() throws Exception {
        when(pisoService.inactivar(PISO)).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch("/api/v1/floors/{id}/deactivate", PISO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void reactivar_devuelveElPisoActivo() throws Exception {
        when(pisoService.reactivar(PISO)).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(patch("/api/v1/floors/{id}/reactivate", PISO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void reactivar_conEdificioInactivo_devuelve422() throws Exception {
        when(pisoService.reactivar(PISO))
                .thenThrow(new ReglaNegocioException("El edificio está inactivo: debe reactivarse primero"));

        mockMvc.perform(patch("/api/v1/floors/{id}/reactivate", PISO))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.mensaje").value("El edificio está inactivo: debe reactivarse primero"));
    }

    @Test
    void listarPorEdificio_conIncluirInactivos_pasaElFiltro() throws Exception {
        when(pisoService.listarPorEdificio(EDIFICIO, true)).thenReturn(List.of(respuesta(Estado.INACTIVO)));

        mockMvc.perform(get("/api/v1/buildings/{id}/floors", EDIFICIO).param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }
}
