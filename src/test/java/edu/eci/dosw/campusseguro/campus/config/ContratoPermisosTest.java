package edu.eci.dosw.campusseguro.campus.config;

import com.jayway.jsonpath.JsonPath;
import edu.eci.dosw.campusseguro.campus.security.RolPermisos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoPermisosTest {

    @Autowired
    private MockMvc mockMvc;

    private record Operacion(HttpMethod metodo, String ruta, String permiso, List<String> roles) {
    }

    private List<Operacion> operaciones() throws Exception {
        String contrato = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(contrato, "$.paths");
        List<Operacion> operaciones = new ArrayList<>();
        paths.forEach((ruta, metodos) -> metodos.forEach((metodo, op) -> {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) op.get("x-roles");
            operaciones.add(new Operacion(HttpMethod.valueOf(metodo.toUpperCase()), ruta,
                    (String) op.get("x-permiso"), roles));
        }));
        return operaciones;
    }

    @Test
    void todasLasOperacionesDeclaranPermisoYRolesCoherentes() throws Exception {
        List<Operacion> operaciones = operaciones();

        assertThat(operaciones).isNotEmpty();
        assertThat(operaciones).allSatisfy(op -> {
            assertThat(op.permiso()).as("x-permiso de %s %s", op.metodo(), op.ruta()).startsWith("ms02:");
            assertThat(op.roles()).as("x-roles de %s %s", op.metodo(), op.ruta())
                    .isEqualTo(RolPermisos.rolesCon(op.permiso()));
        });
    }

    @Test
    void laSeguridadCumpleLoQueDiceElContrato() throws Exception {
        for (Operacion op : operaciones()) {
            for (String rol : RolPermisos.ROLES) {
                int status = mockMvc.perform(peticion(op, rol)).andReturn().getResponse().getStatus();
                if (op.roles().contains(rol)) {
                    assertThat(status).as("%s %s con %s", op.metodo(), op.ruta(), rol).isNotIn(401, 403);
                } else {
                    assertThat(status).as("%s %s con %s", op.metodo(), op.ruta(), rol).isEqualTo(403);
                }
            }
        }
    }

    private static MockHttpServletRequestBuilder peticion(Operacion op, String rol) {
        String ruta = op.ruta().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        MockHttpServletRequestBuilder peticion = request(op.metodo(), ruta)
                .header("X-User-Id", "USR-1034")
                .header("X-User-Role", rol);
        if (op.metodo() != HttpMethod.GET) {
            peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return peticion;
    }
}
