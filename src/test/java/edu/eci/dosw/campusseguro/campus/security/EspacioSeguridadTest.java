package edu.eci.dosw.campusseguro.campus.security;

import edu.eci.dosw.campusseguro.campus.controller.EspacioController;
import edu.eci.dosw.campusseguro.campus.service.EspacioService;
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

@WebMvcTest(EspacioController.class)
@Import(SecurityConfig.class)
class EspacioSeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String ESPACIO_JSON = "{\"codigo\":\"G-101\",\"nombre\":\"Salón 101\",\"tipo\":\"SALON\"}";
    private static final String MODIFICAR_JSON = "{\"nombre\":\"Salón 101\",\"tipo\":\"LABORATORIO\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EspacioService espacioService;

    @Test
    void sinCabeceras_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}", uuid(3)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void estudiante_puedeListarLosEspaciosDeUnPiso() throws Exception {
        mockMvc.perform(get("/api/v1/floors/{id}/spaces", uuid(2))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_puedeListarLosEspaciosDeUnEdificio() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/{id}/spaces", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_puedeConsultarUnEspacio() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}", uuid(3))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeCrearEspacios_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/floors/{id}/spaces", uuid(2))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(ESPACIO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    @Test
    void coordinador_noPuedeModificarEspacios() throws Exception {
        mockMvc.perform(put("/api/v1/spaces/{id}", uuid(3))
                        .header(USER, "USR-1").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(MODIFICAR_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeCrearEspacios() throws Exception {
        mockMvc.perform(post("/api/v1/floors/{id}/spaces", uuid(2))
                        .header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(ESPACIO_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void admin_puedeModificarEspacios() throws Exception {
        mockMvc.perform(put("/api/v1/spaces/{id}", uuid(3))
                        .header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(MODIFICAR_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeInactivarEspacios() throws Exception {
        mockMvc.perform(patch("/api/v1/spaces/{id}/deactivate", uuid(3))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeInactivarEspacios() throws Exception {
        mockMvc.perform(patch("/api/v1/spaces/{id}/deactivate", uuid(3))
                        .header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeReactivarEspacios() throws Exception {
        mockMvc.perform(patch("/api/v1/spaces/{id}/reactivate", uuid(3))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeReactivarEspacios() throws Exception {
        mockMvc.perform(patch("/api/v1/spaces/{id}/reactivate", uuid(3))
                        .header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }
}
