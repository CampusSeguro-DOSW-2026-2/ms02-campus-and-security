package edu.eci.dosw.campusseguro.campus.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiTest {

    private static final String CAMBIAR_ESTADO = "$.paths['/api/v1/evacuation-routes/{id}/status'].patch";

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
                .andExpect(jsonPath("$.paths['/api/v1/floors/{id}/deactivate'].patch.tags[0]").value("Pisos"));
    }

    @Test
    void elContratoSeExportaEnYaml() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk());
    }

    @Test
    void elServidorPrincipalEsElGateway() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"))
                .andExpect(jsonPath("$.servers[1].url").value("http://localhost:8082"));
    }

    @Test
    void laIdentidadSeDocumentaComoCabecerasYNoComoEsquemaDeSeguridad() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist())
                .andExpect(jsonPath("$.security").doesNotExist())
                .andExpect(jsonPath("$.components.parameters.XUserId.name").value("X-User-Id"))
                .andExpect(jsonPath("$.components.parameters.XUserId.required").value(true))
                .andExpect(jsonPath("$.components.parameters.XUserRole.name").value("X-User-Role"))
                .andExpect(jsonPath("$.components.parameters.XCorrelationId.required").value(false))
                .andExpect(jsonPath(CAMBIAR_ESTADO + ".parameters[*].$ref", hasItem("#/components/parameters/XUserId")))
                .andExpect(jsonPath(CAMBIAR_ESTADO + ".parameters[*].$ref",
                        hasItem("#/components/parameters/XCorrelationId")));
    }

    @Test
    void cadaOperacionDeclaraPermisoYRoles() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(CAMBIAR_ESTADO + "['x-permiso']").value("ms02:route_status:write"))
                .andExpect(jsonPath(CAMBIAR_ESTADO + "['x-roles']", contains("COORDINADOR", "ADMIN")));
    }

    @Test
    void losErroresUsanErrorResponse() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.required", hasItem("correlation_id")))
                .andExpect(jsonPath("$.components.responses.Forbidden.content['application/json'].example.codigo")
                        .value("PROHIBIDO"))
                .andExpect(jsonPath(CAMBIAR_ESTADO + ".responses['403'].$ref").value("#/components/responses/Forbidden"))
                .andExpect(jsonPath(CAMBIAR_ESTADO + ".responses['422'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/buildings'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/error']").doesNotExist());
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
