package edu.eci.dosw.campusseguro.campus.security;

import edu.eci.dosw.campusseguro.campus.controller.SalidaController;
import edu.eci.dosw.campusseguro.campus.service.SalidaService;
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

@WebMvcTest(SalidaController.class)
@Import(SecurityConfig.class)
class SalidaSeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String SALIDA_JSON = "{\"pisoId\":\"" + uuid(2) + "\","
            + "\"nombre\":\"Salida principal\",\"latitud\":4.78271,\"longitud\":-74.04263}";
    private static final String MODIFICAR_JSON = "{\"pisoId\":\"" + uuid(2) + "\",\"nombre\":\"Salida\","
            + "\"latitud\":4.78271,\"longitud\":-74.04263}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalidaService salidaService;

    @Test
    void sinCabeceras_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/exits/{id}", uuid(20)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void estudiante_puedeListarLasSalidasDeUnEdificio() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/{id}/exits", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_puedeConsultarUnaSalida() throws Exception {
        mockMvc.perform(get("/api/v1/exits/{id}", uuid(20))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeRegistrarSalidas_devuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/exits", uuid(1))
                        .header(USER, "USR-1034").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(SALIDA_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PROHIBIDO"));
    }

    @Test
    void coordinador_noPuedeModificarSalidas_devuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/exits/{id}", uuid(20))
                        .header(USER, "USR-2").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(MODIFICAR_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void brigadista_noPuedeInactivarSalidas_devuelve403() throws Exception {
        mockMvc.perform(patch("/api/v1/exits/{id}/deactivate", uuid(20))
                        .header(USER, "USR-3").header(ROLE, "BRIGADISTA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeRegistrarSalidas() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/exits", uuid(1))
                        .header(USER, "USR-9").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(SALIDA_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void admin_puedeModificarInactivarYReactivar() throws Exception {
        mockMvc.perform(put("/api/v1/exits/{id}", uuid(20))
                        .header(USER, "USR-9").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(MODIFICAR_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/exits/{id}/deactivate", uuid(20))
                        .header(USER, "USR-9").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/exits/{id}/reactivate", uuid(20))
                        .header(USER, "USR-9").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }
}
