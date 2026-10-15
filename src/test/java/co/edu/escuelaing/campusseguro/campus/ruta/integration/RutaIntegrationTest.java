package co.edu.escuelaing.campusseguro.campus.ruta.integration;

import co.edu.escuelaing.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.port.VerificadorRutas;
import co.edu.escuelaing.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import co.edu.escuelaing.campusseguro.campus.ruta.repository.RutaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class RutaIntegrationTest {

    private static final String RUTAS = "/api/v1/evacuation-routes";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EnMemoriaCampus campus;
    @Autowired
    private RutaRepository rutaRepository;
    @Autowired
    private VerificadorRutas verificador;

    private EdificioInfo edificio;
    private EspacioInfo salon;
    private EspacioInfo laboratorio;
    private ZonaInfo plazoleta;

    @BeforeEach
    void preparar() {
        rutaRepository.deleteAll();
        campus.limpiar();
        edificio = campus.edificio("Edificio A", true);
        salon = campus.espacio(edificio.id(), "Salón 101", true);
        laboratorio = campus.espacio(edificio.id(), "Laboratorio 2", true);
        plazoleta = campus.zona("Plazoleta", true);
    }

    private String cuerpoRuta(Long espacioId, Long zonaId, String estado, Long alternativa) {
        String alt = alternativa == null ? "" : ",\"rutaAlternativaId\":" + alternativa;
        return "{\"espacioOrigenId\":" + espacioId + ",\"zonaSeguraDestinoId\":" + zonaId
                + ",\"descripcion\":\"Salir por la puerta norte\",\"estado\":\"" + estado + "\"" + alt + "}";
    }

    private Long crearRuta(EspacioInfo espacio, String estado) throws Exception {
        return crearRuta(espacio, estado, null);
    }

    private Long crearRuta(EspacioInfo espacio, String estado, Long alternativa) throws Exception {
        MvcResult resultado = mockMvc.perform(post(RUTAS).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRuta(espacio.id(), plazoleta.id(), estado, alternativa)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    void crearLuegoConsultarDevuelveElDestinoYElRecorrido() throws Exception {
        crearRuta(salon, "DISPONIBLE");

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", salon.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneRuta").value(true))
                .andExpect(jsonPath("$.zonaSeguraNombre").value("Plazoleta"))
                .andExpect(jsonPath("$.recorrido").value("Salir por la puerta norte"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void unEspacioNoPuedeTenerDosRutas() throws Exception {
        crearRuta(salon, "DISPONIBLE");

        mockMvc.perform(post(RUTAS).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRuta(salon.id(), plazoleta.id(), "DISPONIBLE", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void noSePuedeCrearRutaConUnaZonaInactiva() throws Exception {
        ZonaInfo inactiva = campus.zona("Cancha", false);

        mockMvc.perform(post(RUTAS).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRuta(salon.id(), inactiva.id(), "DISPONIBLE", null)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void sePuedeGuardarUnaRutaAlternativa() throws Exception {
        Long principal = crearRuta(salon, "DISPONIBLE");

        mockMvc.perform(post(RUTAS).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRuta(laboratorio.id(), plazoleta.id(), "DISPONIBLE", principal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rutaAlternativaId").value(principal.intValue()));
    }

    @Test
    void cambiarElEstadoSeVeReflejadoEnLaConsulta() throws Exception {
        Long id = crearRuta(salon, "DISPONIBLE");

        mockMvc.perform(patch(RUTAS + "/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"));

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", salon.id()))
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void consultarPorEdificioIncluyeEspaciosConYSinRuta() throws Exception {
        crearRuta(salon, "DISPONIBLE");

        MvcResult resultado = mockMvc.perform(get("/api/v1/buildings/{buildingId}/evacuation-routes", edificio.id()))
                .andExpect(status().isOk())
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString();
        List<Object> todas = JsonPath.read(cuerpo, "$");
        List<Object> conRuta = JsonPath.read(cuerpo, "$[?(@.tieneRuta == true)]");
        assertThat(todas).hasSize(2);
        assertThat(conRuta).hasSize(1);
    }

    @Test
    void consultarSinRutaDevuelveElMensajeDeSinRecomendacion() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", laboratorio.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneRuta").value(false))
                .andExpect(jsonPath("$.mensaje").value("No existe una ruta recomendada para la selección."));
    }

    @Test
    void consultarConDestinoQueSeInactivaAvisaAlUsuario() throws Exception {
        crearRuta(salon, "DISPONIBLE");
        campus.inactivarZona(plazoleta.id());

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", salon.id()))
                .andExpect(jsonPath("$.destinoActivo").value(false))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void elVerificadorIndicaQueEspacioZonaYEdificioTienenRuta() throws Exception {
        assertThat(verificador.tieneRutaEspacio(salon.id())).isFalse();
        assertThat(verificador.tieneRutaEdificio(edificio.id())).isFalse();

        crearRuta(salon, "DISPONIBLE");

        assertThat(verificador.tieneRutaEspacio(salon.id())).isTrue();
        assertThat(verificador.tieneRutaEspacio(laboratorio.id())).isFalse();
        assertThat(verificador.tieneRutaZonaSegura(plazoleta.id())).isTrue();
        assertThat(verificador.tieneRutaEdificio(edificio.id())).isTrue();
    }
}
