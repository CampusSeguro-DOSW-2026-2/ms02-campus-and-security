package edu.eci.dosw.campusseguro.campus.ruta.controller;

import java.util.UUID;
import static edu.eci.dosw.campusseguro.campus.Ids.uuid;

import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.dto.CrearRutaRequest;
import edu.eci.dosw.campusseguro.campus.ruta.dto.RutaResponse;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RecursoNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.ReglaNegocioException;
import edu.eci.dosw.campusseguro.campus.ruta.exception.RutaDuplicadaException;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RutaController.class)
class RutaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RutaService rutaService;

    private final UUID rutaId = uuid(10);
    private final UUID espacioId = uuid(11);
    private final UUID zonaId = uuid(12);

    private RutaResponse respuesta(EstadoRuta estado) {
        return new RutaResponse(rutaId, espacioId, zonaId, null, "Por la puerta norte", estado, Instant.now());
    }

    private String json(UUID espacio, UUID zona, String descripcion) {
    return "{\"espacioOrigenId\":\"" + espacio + "\",\"zonaSeguraDestinoId\":\"" + zona
            + "\",\"descripcion\":\"" + descripcion + "\",\"estado\":\"DISPONIBLE\"}";
    }

    @Test
    void crear_devuelve201ConLocation() throws Exception {
        when(rutaService.crear(any(CrearRutaRequest.class))).thenReturn(respuesta(EstadoRuta.DISPONIBLE));

        mockMvc.perform(post("/api/v1/evacuation-routes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(espacioId, zonaId, "Por la puerta norte")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/evacuation-routes/" + rutaId))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void crear_conDatosInvalidosIndicaLosCampos() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.espacioOrigenId").exists())
                .andExpect(jsonPath("$.campos.zonaSeguraDestinoId").exists())
                .andExpect(jsonPath("$.campos.descripcion").exists())
                .andExpect(jsonPath("$.campos.estado").exists());
    }

    @Test
    void crear_conEstadoNoPermitidoDevuelve400() throws Exception {
        String cuerpo = "{\"espacioOrigenId\":\"" + espacioId + "\",\"zonaSeguraDestinoId\":\"" + zonaId
        + "\",\"descripcion\":\"x\",\"estado\":\"CERRADA\"}";

        mockMvc.perform(post("/api/v1/evacuation-routes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_duplicadaDevuelve409() throws Exception {
        when(rutaService.crear(any(CrearRutaRequest.class))).thenThrow(new RutaDuplicadaException(espacioId));

        mockMvc.perform(post("/api/v1/evacuation-routes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(espacioId, zonaId, "x")))
                .andExpect(status().isConflict());
    }

    @Test
    void crear_conEspacioInactivoDevuelve422() throws Exception {
        when(rutaService.crear(any(CrearRutaRequest.class))).thenThrow(new ReglaNegocioException("El espacio está inactivo"));

        mockMvc.perform(post("/api/v1/evacuation-routes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(espacioId, zonaId, "x")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("El espacio está inactivo"));
    }

    @Test
    void cambiarEstado_devuelveLaRutaActualizada() throws Exception {
        when(rutaService.cambiarEstado(rutaId, EstadoRuta.BLOQUEADA)).thenReturn(respuesta(EstadoRuta.BLOQUEADA));

        mockMvc.perform(patch("/api/v1/evacuation-routes/{id}/status", rutaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));
    }

    @Test
    void cambiarEstado_sinEstadoDevuelve400() throws Exception {
        mockMvc.perform(patch("/api/v1/evacuation-routes/{id}/status", rutaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado").exists());
    }

    @Test
    void obtener_rutaInexistenteDevuelve404() throws Exception {
        when(rutaService.obtener(rutaId)).thenThrow(new RecursoNoEncontradoException("La ruta no existe"));

        mockMvc.perform(get("/api/v1/evacuation-routes/{id}", rutaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtener_conIdMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/evacuation-routes/{id}", "no-es-numero"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_devuelveLasRutas() throws Exception {
        when(rutaService.listar()).thenReturn(List.of(respuesta(EstadoRuta.DISPONIBLE)));

        mockMvc.perform(get("/api/v1/evacuation-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(rutaId.toString()));
    }
}
