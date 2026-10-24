package edu.eci.dosw.campusseguro.campus.ruta.integration;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway.EdificioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.VerificadorRutas;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
@Import(TestGatewaysConfig.class)
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

    private String cuerpoRuta(UUID espacioId, UUID zonaId, String estado) {
        return "{\"espacioOrigenId\":\"" + espacioId + "\",\"zonaSeguraDestinoId\":\"" + zonaId
            + "\",\"descripcion\":\"Salir por la puerta norte\",\"estado\":\"" + estado + "\"}";
    }

    private UUID crearRuta(EspacioInfo espacio, String estado) throws Exception {
        MvcResult resultado = mockMvc.perform(post(RUTAS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRuta(espacio.id(), plazoleta.id(), estado)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
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
                        .content(cuerpoRuta(salon.id(), plazoleta.id(), "DISPONIBLE")))
                .andExpect(status().isConflict());
    }

    @Test
    void noSePuedeCrearRutaConUnaZonaInactiva() throws Exception {
        ZonaInfo inactiva = campus.zona("Cancha", false);

        mockMvc.perform(post(RUTAS).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRuta(salon.id(), inactiva.id(), "DISPONIBLE")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cambiarElEstadoSeVeReflejadoEnLaConsulta() throws Exception {
        UUID id = crearRuta(salon, "DISPONIBLE");

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
    void elVerificadorSoloDevuelveRutasDisponiblesORestringidas() throws Exception {
        assertThat(verificador.rutasActivasDeEspacio(salon.id())).isEmpty();
        assertThat(verificador.rutasActivasDeEdificio(edificio.id())).isEmpty();

        UUID rutaSalon = crearRuta(salon, "DISPONIBLE");
        crearRuta(laboratorio, "BLOQUEADA");

        assertThat(verificador.rutasActivasDeEspacio(salon.id())).containsExactly(rutaSalon);
        assertThat(verificador.rutasActivasDeEspacio(laboratorio.id())).isEmpty();
        assertThat(verificador.rutasActivasDeZonaSegura(plazoleta.id())).containsExactly(rutaSalon);
        assertThat(verificador.rutasActivasDeEdificio(edificio.id())).containsExactly(rutaSalon);

        cambiarEstado(rutaSalon, "RESTRINGIDA");
        assertThat(verificador.rutasActivasDeEspacio(salon.id())).containsExactly(rutaSalon);

        cambiarEstado(rutaSalon, "BLOQUEADA");
        assertThat(verificador.rutasActivasDeEspacio(salon.id())).isEmpty();
        assertThat(verificador.rutasActivasDeZonaSegura(plazoleta.id())).isEmpty();
        assertThat(verificador.rutasActivasDeEdificio(edificio.id())).isEmpty();
    }

    private void cambiarEstado(UUID rutaId, String estado) throws Exception {
        mockMvc.perform(patch(RUTAS + "/{id}/status", rutaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + estado + "\"}"))
                .andExpect(status().isOk());
    }
}
