package edu.eci.dosw.campusseguro.campus.config;

import com.jayway.jsonpath.JsonPath;
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

/**
 * Contrato de permisos de MS02 (Estandares §6, MR-07): cada operacion declara el permiso que exige en
 * x-permiso y MS02 autoriza unicamente con X-User-Permission. No se autoriza por nombre de rol, porque
 * los roles son datos de MS01 y cambian en caliente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoPermisosTest {

    private static final String USER = "X-User-Id";
    private static final String ROLE = "X-User-Role";
    private static final String PERM = "X-User-Permission";
    private static final String PUBLICO = "/api/v1/public/";
    private static final String PERMISO_AJENO = "ms02:inventado:leer";

    @Autowired
    private MockMvc mockMvc;

    private record Operacion(HttpMethod metodo, String ruta, String permiso) {
        boolean publica() {
            return ruta.startsWith(PUBLICO);
        }
    }

    private List<Operacion> operaciones() throws Exception {
        String contrato = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(contrato, "$.paths");
        List<Operacion> operaciones = new ArrayList<>();
        paths.forEach((ruta, metodos) -> metodos.forEach((metodo, op) -> operaciones.add(
                new Operacion(HttpMethod.valueOf(metodo.toUpperCase()), ruta, (String) op.get("x-permiso")))));
        return operaciones;
    }

    @Test
    void todasLasOperacionesProtegidasDeclaranSuPermiso() throws Exception {
        List<Operacion> operaciones = operaciones().stream().filter(op -> !op.publica()).toList();

        assertThat(operaciones).isNotEmpty();
        assertThat(operaciones).allSatisfy(op -> assertThat(op.permiso())
                .as("x-permiso de %s %s", op.metodo(), op.ruta())
                .startsWith("ms02:"));
    }

    @Test
    void elPermisoDeclaradoAutorizaYUnPermisoAjenoDeniega() throws Exception {
        for (Operacion op : operaciones()) {
            if (op.publica()) {
                continue;
            }

            int autorizado = mockMvc.perform(peticion(op, op.permiso())).andReturn().getResponse().getStatus();
            assertThat(autorizado).as("%s %s con %s", op.metodo(), op.ruta(), op.permiso()).isNotIn(401, 403);

            int denegado = mockMvc.perform(peticion(op, PERMISO_AJENO)).andReturn().getResponse().getStatus();
            assertThat(denegado).as("%s %s sin su permiso", op.metodo(), op.ruta()).isEqualTo(403);
        }
    }

    @Test
    void sinCabecerasDeIdentidadDevuelve401() throws Exception {
        int status = mockMvc.perform(get("/api/v1/buildings")).andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    void sinLaCabeceraDePermisosDevuelve401() throws Exception {
        int status = mockMvc.perform(get("/api/v1/buildings")
                        .header(USER, "USR-1034")
                        .header(ROLE, "ADMIN"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    void lasOperacionesPublicasSonSoloGetSinPermisoYRespondenSinCabeceras() throws Exception {
        List<Operacion> publicas = operaciones().stream().filter(Operacion::publica).toList();

        assertThat(publicas).extracting(Operacion::ruta)
                .containsExactlyInAnyOrder("/api/v1/public/spaces", "/api/v1/public/exits", "/api/v1/public/safe-zones");
        for (Operacion op : publicas) {
            assertThat(op.metodo()).isEqualTo(HttpMethod.GET);
            assertThat(op.permiso()).as("x-permiso de %s", op.ruta()).isNull();
            int status = mockMvc.perform(get(op.ruta()).param("id", UUID.randomUUID().toString()))
                    .andReturn().getResponse().getStatus();
            assertThat(status).as("%s sin cabeceras", op.ruta()).isNotIn(401, 403);
        }
    }

    private static MockHttpServletRequestBuilder peticion(Operacion op, String permiso) {
        String ruta = op.ruta().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        MockHttpServletRequestBuilder peticion = request(op.metodo(), ruta)
                .header(USER, "USR-1034")
                .header(ROLE, "ADMIN")
                .header(PERM, permiso);
        if (op.metodo() != HttpMethod.GET) {
            peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return peticion;
    }
}
