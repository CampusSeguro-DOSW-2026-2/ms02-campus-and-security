package edu.eci.dosw.campusseguro.campus.security;

import edu.eci.dosw.campusseguro.campus.controller.CodigoQrController;
import edu.eci.dosw.campusseguro.campus.service.CodigoQrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CodigoQrController.class)
@Import(SecurityConfig.class)
class CodigoQrSeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodigoQrService codigoQrService;

    @Test
    void sinCabeceras_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}/qr", uuid(30)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void estudiante_noPuedeGenerarQr() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}/qr", uuid(30)).header(USER, "USR-1").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/exits/{id}/qr", uuid(30)).header(USER, "USR-1").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/safe-zones/{id}/qr", uuid(30)).header(USER, "USR-1").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinador_noPuedeGenerarQr() throws Exception {
        mockMvc.perform(get("/api/v1/safe-zones/{id}/qr", uuid(30)).header(USER, "USR-2").header(ROLE, "COORDINADOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeGenerarLosTresQr() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}/qr", uuid(30)).header(USER, "USR-9").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exits/{id}/qr", uuid(30)).header(USER, "USR-9").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/safe-zones/{id}/qr", uuid(30)).header(USER, "USR-9").header(ROLE, "ADMIN"))
                .andExpect(status().isOk());
    }
}
