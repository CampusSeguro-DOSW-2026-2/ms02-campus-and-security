package edu.eci.dosw.campusseguro.campus.config;

import edu.eci.dosw.campusseguro.campus.service.PisoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstandaresIntegracionTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String CORRELATION = "X-Correlation-ID";
    private static final String UUID_V4 = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PisoService pisoService;

    @Test
    void health_soloDevuelveElEstado() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.groups").doesNotExist())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void info_noEstaExpuesto() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correlationId_seRespetaCuandoLlega() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/floors/{id}", uuid(1)).header(CORRELATION, id))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CORRELATION, id))
                .andExpect(jsonPath("$.correlation_id").value(id));
    }

    @Test
    void correlationId_seGeneraSiNoLlega() throws Exception {
        String generado = mockMvc.perform(get("/api/v1/floors/{id}", uuid(1)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CORRELATION, matchesPattern(UUID_V4)))
                .andExpect(jsonPath("$.correlation_id").value(matchesPattern(UUID_V4)))
                .andReturn().getResponse().getHeader(CORRELATION);
        assertThat(generado).isNotBlank();
    }

    @Test
    void correlationId_invalido_seReemplaza() throws Exception {
        mockMvc.perform(get("/api/v1/floors/{id}", uuid(1)).header(CORRELATION, "abc"))
                .andExpect(header().string(CORRELATION, matchesPattern(UUID_V4)))
                .andExpect(jsonPath("$.correlation_id").value(matchesPattern(UUID_V4)));
    }

    @Test
    void rutaInexistente_devuelve404ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe").header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.errores").isArray())
                .andExpect(jsonPath("$.correlation_id").value(matchesPattern(UUID_V4)));
    }

    @Test
    void metodoNoPermitido_devuelve405() throws Exception {
        mockMvc.perform(delete("/api/v1/floors/{id}", uuid(1)).header(USER, "USR-1").header(ROLE, "ADMIN"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.correlation_id").exists());
    }

    @Test
    void tipoDeContenidoNoSoportado_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/buildings/{id}/floors", uuid(1))
                        .header(USER, "USR-1").header(ROLE, "ADMIN")
                        .contentType(MediaType.TEXT_PLAIN).content("piso"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.correlation_id").exists());
    }

    @Test
    void errorInesperado_devuelve500SinDetallesInternos() throws Exception {
        when(pisoService.obtener(uuid(1))).thenThrow(new IllegalStateException("conexión rota a la base"));

        mockMvc.perform(get("/api/v1/floors/{id}", uuid(1)).header(USER, "USR-1").header(ROLE, "ESTUDIANTE"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value(matchesPattern("^(?!.*conexión).*$")))
                .andExpect(jsonPath("$.errores").isArray())
                .andExpect(jsonPath("$.correlation_id").exists());
    }
}
