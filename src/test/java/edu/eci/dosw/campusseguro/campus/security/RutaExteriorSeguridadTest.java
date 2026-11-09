package edu.eci.dosw.campusseguro.campus.security;

import java.util.List;

import edu.eci.dosw.campusseguro.campus.ruta.controller.RutaExteriorController;
import edu.eci.dosw.campusseguro.campus.ruta.dto.ResumenGeneracionResponse;
import edu.eci.dosw.campusseguro.campus.ruta.service.GeneradorRutasExterioresService;
import edu.eci.dosw.campusseguro.campus.ruta.service.RutaExteriorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RutaExteriorController.class)
@Import(SecurityConfig.class)
class RutaExteriorSeguridadTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String CUERPO = "{\"salidaId\":\"" + uuid(30) + "\",\"zonaSeguraId\":\"" + uuid(31) + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RutaExteriorService rutaExteriorService;

    @MockitoBean
    private GeneradorRutasExterioresService generador;

    @Test
    void sinCabeceras_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/exits/{id}/evacuation-route", uuid(30)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void estudiante_puedeConsultarLaRutaExteriorDeUnaSalida() throws Exception {
        when(rutaExteriorService.listarPorSalida(uuid(30))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/exits/{id}/evacuation-route", uuid(30))
                        .header(USER, "USR-1").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isOk());
    }

    @Test
    void estudiante_noPuedeCalcularRutas() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes/exterior")
                        .header(USER, "USR-1").header(ROLE, "ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinador_noPuedeCalcularRutas() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes/exterior")
                        .header(USER, "USR-2").header(ROLE, "COORDINADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeCalcularRutas() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes/exterior")
                        .header(USER, "USR-3").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
    }

    @Test
    void admin_conCuerpoIncompleto_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes/exterior")
                        .header(USER, "USR-3").header(ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void estudiante_noPuedeGenerarRutas() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes/exterior/generate")
                        .header(USER, "USR-1").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinador_noPuedeGenerarRutas() throws Exception {
        mockMvc.perform(post("/api/v1/evacuation-routes/exterior/generate")
                        .header(USER, "USR-2").header(ROLE, "COORDINADOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeGenerarRutas() throws Exception {
        when(generador.generarTodas(false)).thenReturn(new ResumenGeneracionResponse(4, 1, false));

        mockMvc.perform(post("/api/v1/evacuation-routes/exterior/generate")
                        .header(USER, "USR-3").header(ROLE, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.generadas").value(4));
    }

    @Test
    void admin_puedePedirRecalcularTodo() throws Exception {
        when(generador.generarTodas(true)).thenReturn(new ResumenGeneracionResponse(6, 0, false));

        mockMvc.perform(post("/api/v1/evacuation-routes/exterior/generate?recalcular=true")
                        .header(USER, "USR-3").header(ROLE, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.generadas").value(6));
    }
}
