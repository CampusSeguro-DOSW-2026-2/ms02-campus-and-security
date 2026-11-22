package edu.eci.dosw.campusseguro.campus.ruta.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.entity.Campus;
import edu.eci.dosw.campusseguro.campus.entity.Edificio;
import edu.eci.dosw.campusseguro.campus.entity.Estado;
import edu.eci.dosw.campusseguro.campus.entity.Piso;
import edu.eci.dosw.campusseguro.campus.entity.Salida;
import edu.eci.dosw.campusseguro.campus.repository.CampusRepository;
import edu.eci.dosw.campusseguro.campus.repository.EdificioRepository;
import edu.eci.dosw.campusseguro.campus.repository.PisoRepository;
import edu.eci.dosw.campusseguro.campus.repository.SalidaRepository;
import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
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

// La guía automática (PR "fix/evacuation-summary-from-guide") necesita salida + piso reales en la
// base (los gateways de salida/escalera sí consultan la base de verdad, a diferencia de
// EdificioGateway/EspacioGateway/ZonaSeguraGateway, que aquí son falsos). Por eso, además del campus
// en memoria, este test siembra Campus/Edificio/Piso/Salida reales con el MISMO id de edificio que
// el edificio falso, y una ruta exterior guardada directamente (sin pasar por el proveedor de
// navegación real, que haría una llamada de red).
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
    @Autowired
    private CampusRepository campusRepository;
    @Autowired
    private EdificioRepository edificioRepository;
    @Autowired
    private PisoRepository pisoRepository;
    @Autowired
    private SalidaRepository salidaRepository;

    private EdificioInfo edificio;
    private EspacioInfo salon;
    private EspacioInfo laboratorio;
    private ZonaInfo plazoleta;
    private UUID salidaId;

    @BeforeEach
    void preparar() {
        rutaRepository.deleteAll();
        salidaRepository.deleteAll();
        pisoRepository.deleteAll();
        edificioRepository.deleteAll();
        campusRepository.deleteAll();
        campus.limpiar();

        Campus campusReal = new Campus();
        campusReal.setNombre("Campus principal");
        campusReal.setEstado(Estado.ACTIVO);
        campusReal = campusRepository.save(campusReal);

        Edificio edificioReal = new Edificio();
        edificioReal.setNombre("Edificio A");
        edificioReal.setCampusId(campusReal.getId());
        edificioReal.setEstado(Estado.ACTIVO);
        edificioReal = edificioRepository.save(edificioReal);
        edificio = campus.edificioConId(edificioReal.getId(), "Edificio A", true);

        // Piso 1: tiene la salida con ruta exterior; "salon" vive ahí, así no necesita escalera.
        Piso piso1 = new Piso();
        piso1.setEdificioId(edificio.id());
        piso1.setNumero(1);
        piso1.setEstado(Estado.ACTIVO);
        piso1 = pisoRepository.save(piso1);

        Salida salidaReal = new Salida();
        salidaReal.setEdificioId(edificio.id());
        salidaReal.setPisoId(piso1.getId());
        salidaReal.setNombre("Puerta norte");
        salidaReal.setLatitud(new BigDecimal("4.782710"));
        salidaReal.setLongitud(new BigDecimal("-74.042630"));
        salidaReal.setEstado(Estado.ACTIVO);
        salidaId = salidaRepository.save(salidaReal).getId();

        // Piso 2: sin salida ni escalera que lo conecte con el piso 1; "laboratorio" vive ahí, para
        // seguir teniendo un espacio sin ruta recomendada.
        Piso piso2 = new Piso();
        piso2.setEdificioId(edificio.id());
        piso2.setNumero(2);
        piso2.setEstado(Estado.ACTIVO);
        piso2 = pisoRepository.save(piso2);

        salon = campus.espacioEnPiso(edificio.id(), "Salón 101", true, piso1.getId(), 1);
        laboratorio = campus.espacioEnPiso(edificio.id(), "Laboratorio 2", true, piso2.getId(), 2);
        plazoleta = campus.zona("Plazoleta", true);
    }

    // Guarda directamente la ruta exterior ya calculada (salida -> zona), sin pasar por el
    // proveedor de navegación real.
    private RutaEvacuacion guardarRutaExterior(double metros, EstadoRuta estado) {
        RutaEvacuacion ruta = RutaEvacuacion.exterior(salidaId, plazoleta.id(), "Ruta exterior");
        // Pasos en el formato de CodificadorRutaExterior: "distancia|duración|instrucción" por línea.
        ruta.registrarCalculo(metros, metros / 1.4,
                "4.78271,-74.04263;4.7831,-74.0431", metros + "|" + (metros / 1.4) + "|Siga recto", Instant.now());
        if (estado != EstadoRuta.DISPONIBLE) {
            ruta.cambiarEstado(estado, "Obra en la escalera");
        }
        return rutaRepository.save(ruta);
    }

    private static String motivoJson(String estado) {
        return "DISPONIBLE".equals(estado)
                ? ""
                : ",\"motivo\":\"Obra en la escalera\"";
    }

    private String cuerpoRuta(UUID espacioId, UUID zonaId, String estado) {
        return "{\"espacioOrigenId\":\"" + espacioId + "\",\"zonaSeguraDestinoId\":\"" + zonaId
                + "\",\"descripcion\":\"Salir por la puerta norte\",\"estado\":\"" + estado + "\""
                + motivoJson(estado) + "}";
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
    void existeRutaExteriorLuegoConsultarDevuelveElDestinoYElRecorrido() throws Exception {
        guardarRutaExterior(280, EstadoRuta.DISPONIBLE);

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", salon.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneRuta").value(true))
                .andExpect(jsonPath("$.zonaSeguraNombre").value("Plazoleta"))
                .andExpect(jsonPath("$.recorrido").exists())
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
        UUID id = guardarRutaExterior(280, EstadoRuta.DISPONIBLE).getId();

        mockMvc.perform(patch(RUTAS + "/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"BLOQUEADA\",\"motivo\":\"Obra en la escalera\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.motivo").value("Obra en la escalera"));

        // Bloqueada o no, la ruta se sigue recomendando (el usuario necesita saber por dónde sería)
        // pero con el aviso correspondiente, en vez de mostrar que no hay ruta.
        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", salon.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneRuta").value(true))
                .andExpect(jsonPath("$.estado").value("BLOQUEADA"))
                .andExpect(jsonPath("$.motivo").value("Obra en la escalera"))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void consultarPorEdificioIncluyeEspaciosConYSinRuta() throws Exception {
        guardarRutaExterior(280, EstadoRuta.DISPONIBLE);

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
    void consultarSinSalidaAlcanzableDevuelveUnMensajeClaro() throws Exception {
        // "laboratorio" está en un piso sin salida propia y sin escalera que lo conecte con el
        // piso de la salida, así que no hay ruta que recomendar.
        guardarRutaExterior(280, EstadoRuta.DISPONIBLE);

        mockMvc.perform(get("/api/v1/spaces/{spaceId}/evacuation-route", laboratorio.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneRuta").value(false))
                .andExpect(jsonPath("$.mensaje").value(
                        "No hay una escalera disponible para bajar desde este piso. "
                                + "Siga las indicaciones del personal de emergencia."));
    }

    @Test
    void consultarConDestinoQueSeInactivaAvisaAlUsuario() throws Exception {
        guardarRutaExterior(280, EstadoRuta.DISPONIBLE);
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
                        .content("{\"estado\":\"" + estado + "\"" + motivoJson(estado) + "}"))
                .andExpect(status().isOk());
    }
}
