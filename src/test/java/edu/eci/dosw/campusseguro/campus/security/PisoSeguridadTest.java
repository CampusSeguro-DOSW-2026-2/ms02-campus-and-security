package edu.eci.dosw.campusseguro.campus.security;

import edu.eci.dosw.campusseguro.campus.controller.PisoController;
import edu.eci.dosw.campusseguro.campus.service.PisoService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Seguridad real de los endpoints de pisos: lectura para cualquier rol, escritura solo ADMIN.
@WebMvcTest(PisoController.class)
@Import(SecurityConfig.class)
class PisoSeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String PISO_JSON = "{\"numero\":1,\"nombre\":\"Primer piso\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PisoService pisoService;

    @Test
    void sinCabeceras_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/{id}/floors", uuid(1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void estudiante_puedeListarLosPisosDeUnEdificio() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/{id}/floors", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_puedeConsultarUnPiso() throws Exception {
        mockMvc.perform(get("/api/v1/floors/{id}", uuid(2))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeCrearPisos_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/floors", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(PISO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    @Test
    void coordinador_noPuedeModificarPisos() throws Exception {
        mockMvc.perform(put("/api/v1/floors/{id}", uuid(2))
                        .header(USER, "USR-1").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(PISO_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeCrearPisos() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/floors", uuid(1))
                        .header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(PISO_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void admin_puedeInactivarPisos() throws Exception {
        mockMvc.perform(patch("/api/v1/floors/{id}/deactivate", uuid(2))
                        .header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }
}
