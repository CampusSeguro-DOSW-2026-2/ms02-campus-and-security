package edu.eci.dosw.campusseguro.campus.security;

import edu.eci.dosw.campusseguro.campus.controller.EdificioController;
import edu.eci.dosw.campusseguro.campus.ruta.controller.RutaController;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaService;
import edu.eci.dosw.campusseguro.campus.service.EdificioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Aqui NO se desactivan los filtros: se prueba la seguridad real (cabeceras, roles y permisos).
@WebMvcTest({EdificioController.class, RutaController.class})
@Import(SecurityConfig.class)
class SeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String EDIFICIO_JSON = "{\"codigo\":\"A\",\"nombre\":\"Edificio A\"}";
    private static final String ESTADO_JSON = "{\"estado\":\"BLOQUEADA\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EdificioService edificioService;

    @MockitoBean
    private RutaService rutaService;

    @Test
    void sinCabeceras_devuelve401ConElFormatoDeError() throws Exception {
        mockMvc.perform(get("/api/v1/buildings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void soloConUserIdSinRol_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/buildings").header(USER, "USR-1034"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void estudiante_puedeConsultarEdificios() throws Exception {
        mockMvc.perform(get("/api/v1/buildings").header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeCrearEdificios_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/buildings").header(USER, "USR-1034").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(EDIFICIO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    @Test
    void admin_puedeCrearEdificiosSinTokenCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/buildings").header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(EDIFICIO_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void conVariosRoles_seUnenSusPermisos() throws Exception {
        mockMvc.perform(post("/api/v1/buildings").header(USER, "USR-1").header(ROLE, "ESTUDIANTE, ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(EDIFICIO_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void rolDesconocido_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/buildings").header(USER, "USR-1").header(ROLE, "INVENTADO"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditor_puedeConsultarRutas() throws Exception {
        mockMvc.perform(get("/api/v1/evacuation-routes").header(USER, "USR-1").header(ROLE, "AUDITOR"))
                .andExpect(status().isOk());
    }

    @Test
    void coordinador_puedeCambiarElEstadoDeUnaRuta() throws Exception {
        mockMvc.perform(patch("/api/v1/evacuation-routes/{id}/status", uuid(7))
                        .header(USER, "USR-1").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(ESTADO_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeCambiarElEstadoDeUnaRuta() throws Exception {
        mockMvc.perform(patch("/api/v1/evacuation-routes/{id}/status", uuid(7))
                        .header(USER, "USR-1").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(ESTADO_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinador_noPuedeCrearRutas() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes").header(USER, "USR-1").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
