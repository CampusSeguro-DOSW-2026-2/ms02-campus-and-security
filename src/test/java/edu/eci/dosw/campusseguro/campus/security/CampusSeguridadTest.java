package edu.eci.dosw.campusseguro.campus.security;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.eci.dosw.campusseguro.campus.controller.CampusController;
import edu.eci.dosw.campusseguro.campus.controller.ZonaAbiertaController;
import edu.eci.dosw.campusseguro.campus.service.CampusService;
import edu.eci.dosw.campusseguro.campus.service.ZonaAbiertaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Seguridad real de campus y zonas abiertas: lectura para cualquier rol, escritura solo ADMIN.
@WebMvcTest({CampusController.class, ZonaAbiertaController.class})
@Import(SecurityConfig.class)
class CampusSeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String CAMPUS_JSON = "{\"nombre\":\"Campus principal\"}";
    private static final String ZONA_JSON = "{\"nombre\":\"Plazoleta\",\"tipo\":\"PLAZOLETA\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampusService campusService;

    @MockitoBean
    private ZonaAbiertaService zonaAbiertaService;

    @Test
    void sinCabeceras_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/campuses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void estudiante_puedeListarYConsultarCampus() throws Exception {
        mockMvc.perform(get("/api/v1/campuses").header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/campuses/{id}", uuid(1)).header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_puedeConsultarZonasAbiertas() throws Exception {
        mockMvc.perform(get("/api/v1/campuses/{id}/open-zones", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/open-zones/{id}", uuid(2)).header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeCrearCampus_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/campuses").header(USER, "USR-1034").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(CAMPUS_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    @Test
    void coordinador_noPuedeModificarCampus() throws Exception {
        mockMvc.perform(put("/api/v1/campuses/{id}", uuid(1)).header(USER, "USR-1").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Campus\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiante_noPuedeCrearZonasAbiertas_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/campuses/{id}/open-zones", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(ZONA_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudiante_noPuedeInactivarNiReactivarZonasAbiertas() throws Exception {
        mockMvc.perform(patch("/api/v1/open-zones/{id}/deactivate", uuid(2))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/open-zones/{id}/reactivate", uuid(2))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeCrearCampusYZonasAbiertas() throws Exception {
        mockMvc.perform(post("/api/v1/campuses").header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(CAMPUS_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/campuses/{id}/open-zones", uuid(1)).header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(ZONA_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void admin_puedeInactivarYReactivarCampus() throws Exception {
        mockMvc.perform(patch("/api/v1/campuses/{id}/deactivate", uuid(1)).header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/campuses/{id}/reactivate", uuid(1)).header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }
}
