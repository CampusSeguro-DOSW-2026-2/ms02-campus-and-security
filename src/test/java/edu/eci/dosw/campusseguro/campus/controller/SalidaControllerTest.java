package edu.eci.dosw.campusseguro.campus.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.dto.ModificarSalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaRequest;
import edu.eci.dosw.campusseguro.campus.dto.SalidaResponse;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.service.SalidaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SalidaController.class)
class SalidaControllerTest {

    private static final UUID EDIFICIO = uuid(1);
    private static final UUID PISO = uuid(2);
    private static final UUID SALIDA = uuid(20);
    private static final String SALIDA_JSON = "{\"pisoId\":\"" + PISO + "\","
            + "\"nombre\":\"Salida principal\",\"latitud\":4.78271,\"longitud\":-74.04263}";
    private static final String MODIFICAR_JSON = "{\"pisoId\":\"" + PISO + "\",\"nombre\":\"Salida norte\","
            + "\"latitud\":4.78271,\"longitud\":-74.04263}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalidaService salidaService;

    private SalidaResponse respuesta(Estado estado) {
        return new SalidaResponse(SALIDA, EDIFICIO, "Bloque G", PISO, 1, "Salida principal",
                new BigDecimal("4.782710"), new BigDecimal("-74.042630"), estado, estado == Estado.ACTIVO, false);
    }

    @Test
    void crear_devuelve201ConLasCoordenadas() throws Exception {
        when(salidaService.crear(eq(EDIFICIO), any(SalidaRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post("/api/v1/buildings/{id}/exits", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content(SALIDA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Salida principal"))
                .andExpect(jsonPath("$.latitud").value(4.78271))
                .andExpect(jsonPath("$.longitud").value(-74.04263))
                .andExpect(jsonPath("$.pisoNumero").value(1))
                .andExpect(jsonPath("$.edificioNombre").value("Bloque G"));
    }

    @Test
    void crear_sinCamposObligatorios_devuelve400ConLosCampos() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/exits", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'pisoId')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'latitud')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'longitud')]").exists());
    }

    @Test
    void crear_conCoordenadasFueraDeRango_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/exits", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pisoId\":\"" + PISO + "\",\"nombre\":\"Salida\","
                                + "\"latitud\":95,\"longitud\":-190}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[?(@.campo == 'latitud')].mensaje").value("La latitud debe estar entre -90 y 90"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'longitud')].mensaje").value("La longitud debe estar entre -180 y 180"));
    }

    @Test
    void crear_conPisoDeOtroEdificio_devuelve422() throws Exception {
        when(salidaService.crear(eq(EDIFICIO), any(SalidaRequest.class)))
                .thenThrow(new ReglaNegocioException("El piso no pertenece al edificio de la salida"));

        mockMvc.perform(post("/api/v1/buildings/{id}/exits", EDIFICIO).contentType(MediaType.APPLICATION_JSON)
                        .content(SALIDA_JSON))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }

    @Test
    void listarPorEdificio_devuelveLasSalidas() throws Exception {
        when(salidaService.listarPorEdificio(EDIFICIO, false)).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/buildings/{id}/exits", EDIFICIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SALIDA.toString()));
    }

    @Test
    void listarPorEdificio_conIncluirInactivos_pasaElFiltro() throws Exception {
        when(salidaService.listarPorEdificio(EDIFICIO, true)).thenReturn(List.of(respuesta(Estado.INACTIVO)));

        mockMvc.perform(get("/api/v1/buildings/{id}/exits", EDIFICIO).param("incluirInactivos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponible").value(false));
    }

    @Test
    void obtener_salidaInexistente_devuelve404() throws Exception {
        when(salidaService.obtener(SALIDA)).thenThrow(new SalidaNoEncontradaException(SALIDA));

        mockMvc.perform(get("/api/v1/exits/{id}", SALIDA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void modificar_devuelveLaSalidaActualizada() throws Exception {
        when(salidaService.modificar(eq(SALIDA), any(ModificarSalidaRequest.class))).thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put("/api/v1/exits/{id}", SALIDA).contentType(MediaType.APPLICATION_JSON)
                        .content(MODIFICAR_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SALIDA.toString()));
    }

    @Test
    void inactivar_devuelveLaSalidaInactiva() throws Exception {
        when(salidaService.inactivar(SALIDA)).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch("/api/v1/exits/{id}/deactivate", SALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void reactivar_conPisoInactivo_devuelve422() throws Exception {
        when(salidaService.reactivar(SALIDA))
                .thenThrow(new ReglaNegocioException("El piso o el edificio está inactivo: debe reactivarse primero"));

        mockMvc.perform(patch("/api/v1/exits/{id}/reactivate", SALIDA))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
    }
}
