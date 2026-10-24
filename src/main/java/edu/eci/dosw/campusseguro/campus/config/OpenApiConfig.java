package edu.eci.dosw.campusseguro.campus.config;

import edu.eci.dosw.campusseguro.campus.security.HeaderAuthenticationFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String USUARIO = "usuario";
    private static final String ROL = "rol";

    @Bean
    public OpenAPI campusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MS02 Campus y seguridad")
                        .version("v1")
                        .description("Edificios, pisos, espacios, zonas seguras y rutas de evacuación de CampusSeguro. "
                                + "La identidad llega del API Gateway en las cabeceras X-User-Id y X-User-Role."))
                .components(new Components()
                        .addSecuritySchemes(USUARIO, cabecera(HeaderAuthenticationFilter.USER_ID, "Id del usuario"))
                        .addSecuritySchemes(ROL, cabecera(HeaderAuthenticationFilter.USER_ROLE,
                                "Roles separados por coma, por ejemplo ADMIN o ESTUDIANTE")))
                .addSecurityItem(new SecurityRequirement().addList(USUARIO).addList(ROL));
    }

    private static SecurityScheme cabecera(String nombre, String descripcion) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(nombre)
                .description(descripcion);
    }
}
