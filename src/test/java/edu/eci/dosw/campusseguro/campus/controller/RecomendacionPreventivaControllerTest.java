package edu.eci.dosw.campusseguro.campus.controller;

import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaRequest;
import edu.eci.dosw.campusseguro.campus.dto.RecomendacionPreventivaResponse;
import edu.eci.dosw.campusseguro.campus.entity.AmbitoRecomendacion;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.service.RecomendacionPreventivaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RecomendacionPreventivaController.class)
class RecomendacionPreventivaControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID EDIFICIO = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecomendacionPreventivaService service;

    private RecomendacionPreventivaResponse respuesta(Estado estado) {
        return new RecomendacionPreventivaResponse(
                ID,
                AmbitoRecomendacion.EDIFICIO,
                EDIFICIO,
                null,
                "Evacuación segura",
                "Mantenga libres las salidas de emergencia.",
                estado,
                null,
                null
        );
    }

    private String requestJson() {
        return """
                {
                  "ambito": "EDIFICIO",
                  "edificioId": "%s",
                  "titulo": "Evacuación segura",
                  "recomendacion": "Mantenga libres las salidas de emergencia."
                }
                """.formatted(EDIFICIO);
    }

    @Test
    void listar_devuelveRecomendacionesActivas() throws Exception {
        when(service.listarActivas()).thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/preventive-recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Evacuación segura"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
    }

    @Test
    void consultarPorEdificio_devuelveRecomendaciones() throws Exception {
        when(service.consultarPorEdificio(EDIFICIO))
                .thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/preventive-recommendations/building/{buildingId}", EDIFICIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].edificioId").value(EDIFICIO.toString()));
    }

    @Test
    void consultarPorTipo_devuelveRecomendaciones() throws Exception {
        when(service.consultarPorTipo(TipoEspacio.SALON))
                .thenReturn(List.of(respuesta(Estado.ACTIVO)));

        mockMvc.perform(get("/api/v1/preventive-recommendations/space-type/SALON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Evacuación segura"));

        verify(service).consultarPorTipo(TipoEspacio.SALON);
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(service.crear(any(RecomendacionPreventivaRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(post("/api/v1/preventive-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crearSinTitulo_devuelve400() throws Exception {
        String json = """
                {
                  "ambito": "EDIFICIO",
                  "edificioId": "%s",
                  "titulo": " ",
                  "recomendacion": "Mantenga libres las salidas."
                }
                """.formatted(EDIFICIO);

        mockMvc.perform(post("/api/v1/preventive-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinAmbito_devuelve400() throws Exception {
        String json = """
                {
                  "edificioId": "%s",
                  "titulo": "Evacuación segura",
                  "recomendacion": "Mantenga libres las salidas."
                }
                """.formatted(EDIFICIO);

        mockMvc.perform(post("/api/v1/preventive-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificar_devuelveRecomendacionActualizada() throws Exception {
        when(service.modificar(eq(ID), any(RecomendacionPreventivaRequest.class)))
                .thenReturn(respuesta(Estado.ACTIVO));

        mockMvc.perform(put("/api/v1/preventive-recommendations/{id}", ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Evacuación segura"));
    }

    @Test
    void inactivar_devuelveEstadoInactivo() throws Exception {
        when(service.inactivar(ID)).thenReturn(respuesta(Estado.INACTIVO));

        mockMvc.perform(patch("/api/v1/preventive-recommendations/{id}/deactivate", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void consultarConTipoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/preventive-recommendations/space-type/NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }
}
