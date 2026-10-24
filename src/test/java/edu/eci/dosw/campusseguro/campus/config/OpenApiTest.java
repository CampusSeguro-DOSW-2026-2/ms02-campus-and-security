package edu.eci.dosw.campusseguro.campus.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elContratoSeConsultaSinCabeceras() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MS02 Campus y seguridad"))
                .andExpect(jsonPath("$.paths['/api/v1/buildings']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/floors/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/spaces/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/floors/{id}/deactivate'].patch.summary").value("Inactivar piso"))
                .andExpect(jsonPath("$.paths['/api/v1/floors/{id}/deactivate'].patch.tags[0]").value("Pisos"))
                .andExpect(jsonPath("$.components.securitySchemes.usuario.name").value("X-User-Id"))
                .andExpect(jsonPath("$.components.securitySchemes.rol.name").value("X-User-Role"));
    }

    @Test
    void swaggerUiSeAbreSinCabeceras() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void laApiSigueExigiendoCabeceras() throws Exception {
        mockMvc.perform(get("/api/v1/buildings"))
                .andExpect(status().isUnauthorized());
    }
}
