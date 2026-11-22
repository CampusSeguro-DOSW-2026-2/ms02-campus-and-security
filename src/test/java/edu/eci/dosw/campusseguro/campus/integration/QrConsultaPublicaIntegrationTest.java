package edu.eci.dosw.campusseguro.campus.integration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.jayway.jsonpath.JsonPath;
import edu.eci.dosw.campusseguro.campus.ruta.domain.RutaEvacuacion;
import edu.eci.dosw.campusseguro.campus.ruta.repository.RutaRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QrConsultaPublicaIntegrationTest {

    private static final String BASE_QR = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RutaRepository rutaRepository;

    private String edificio;
    private String piso1;
    private String piso2;
    private String espacio;
    private String salida;
    private String zona;

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-User-Id", "USR-ADMIN").header("X-User-Role", "ADMIN");
    }

    private String crear(String url, String json) throws Exception {
        String cuerpo = mockMvc.perform(comoAdmin(post(url)).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private String enlaceDelQr(String url) throws Exception {
        byte[] png = mockMvc.perform(comoAdmin(get(url)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        return new QRCodeReader()
                .decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen))),
                        Map.of(DecodeHintType.PURE_BARCODE, Boolean.TRUE))
                .getText();
    }

    // Lo que hace la pantalla pública del frontend: toma tipo e id de #/qr/{tipo}/{id} y llama a /api/v1/public.
    private static MockHttpServletRequestBuilder abrirSinSesion(String enlace) {
        String[] partes = URI.create(enlace).getFragment().split("/");
        String recurso = Map.of("espacios", "spaces", "salidas", "exits", "zonas", "safe-zones").get(partes[2]);
        return get("/api/v1/public/" + recurso).param("id", partes[3]);
    }

    @BeforeEach
    void registrarElCampus() throws Exception {
        String campus = crear("/api/v1/campuses", "{\"nombre\":\"Campus principal\"}");
        edificio = crear("/api/v1/buildings", "{\"nombre\":\"Bloque G\",\"campusId\":\"" + campus + "\"}");
        piso1 = crear("/api/v1/buildings/" + edificio + "/floors", "{\"numero\":1,\"nombre\":\"Primer piso\"}");
        piso2 = crear("/api/v1/buildings/" + edificio + "/floors", "{\"numero\":2}");
        espacio = crear("/api/v1/floors/" + piso2 + "/spaces",
                "{\"nombre\":\"Salón 201\",\"tipo\":\"SALON\"}");
        salida = crear("/api/v1/buildings/" + edificio + "/exits", "{\"pisoId\":\"" + piso1 + "\","
                + "\"nombre\":\"Salida principal\",\"latitud\":4.78271,\"longitud\":-74.04263}");
        zona = crear("/api/v1/safe-zones", "{\"codigo\":\"QR-MP-01\",\"nombre\":\"Plazoleta H\","
                + "\"ubicacion\":\"Frente al bloque H\",\"descripcion\":\"Punto de encuentro\",\"capacidad\":300,"
                + "\"latitud\":4.7831,\"longitud\":-74.0431}");

        // El resumen de evacuacion del espacio ya no sale de la ruta manual espacio -> zona (se va a
        // dejar de crear, correccion 5): ahora sale de la guia automatica (salida -> ruta exterior). Se
        // crea una salida en el piso del espacio (para no necesitar escalera) y se guarda directamente el
        // ultimo calculo de la ruta exterior, en vez de pasar por el proveedor de navegacion real (que
        // haria una llamada de red).
        String salidaDelEspacio = crear("/api/v1/buildings/" + edificio + "/exits", "{\"pisoId\":\"" + piso2 + "\","
                + "\"nombre\":\"Salida G2\",\"latitud\":4.78305,\"longitud\":-74.04315}");
        RutaEvacuacion rutaExterior = RutaEvacuacion.exterior(
                UUID.fromString(salidaDelEspacio), UUID.fromString(zona), "Ruta exterior");
        // Pasos en el formato de CodificadorRutaExterior: "distancia|duración|instrucción" por línea.
        rutaExterior.registrarCalculo(100, 100 / 1.4,
                "4.78305,-74.04315;4.7831,-74.0431", "100|71.43|Siga recto", Instant.now());
        rutaRepository.save(rutaExterior);

        // TODO(correccion-5-PR2): esta ruta manual espacio -> zona se deja viva solo para que
        // VerificadorRutasService (que todavia solo mira espacio_origen_id) siga bloqueando la
        // desactivacion del espacio en espacioConRutaActiva_noSePuedeInactivarYSigueDisponibleEnLaConsultaPublica.
        // Cuando el PR 2 quite POST /evacuation-routes, ese guardia se queda viendo siempre cero rutas
        // activas (un espacio/edificio con ruta exterior activa se podria desactivar sin aviso); hay que
        // decidir con el dueno de la desactivacion de espacios/edificios si VerificadorRutasService debe
        // mirar tambien las rutas exteriores alcanzables desde el espacio.
        crear("/api/v1/evacuation-routes", "{\"espacioOrigenId\":\"" + espacio + "\",\"zonaSeguraDestinoId\":\"" + zona
                + "\",\"descripcion\":\"Baje por la escalera norte y salga por la puerta principal.\","
                + "\"estado\":\"DISPONIBLE\"}");
    }

    @Test
    void espacio_delQrALaConsultaPublica() throws Exception {
        String enlace = enlaceDelQr("/api/v1/spaces/" + espacio + "/qr");

        assertThat(enlace).isEqualTo(BASE_QR + "/#/qr/espacios/" + espacio);
        mockMvc.perform(abrirSinSesion(enlace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Salón 201"))
                .andExpect(jsonPath("$.edificio").value("Bloque G"))
                .andExpect(jsonPath("$.piso").value(2))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.evacuacion.zonaSegura").value("Plazoleta H"))
                .andExpect(jsonPath("$.evacuacion.recorrido", Matchers.containsString("Plazoleta H")))
                .andExpect(jsonPath("$.evacuacion.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.edificioId").doesNotExist());
    }

    @Test
    void salida_delQrALaConsultaPublica() throws Exception {
        String enlace = enlaceDelQr("/api/v1/exits/" + salida + "/qr");

        assertThat(enlace).isEqualTo(BASE_QR + "/#/qr/salidas/" + salida);
        mockMvc.perform(abrirSinSesion(enlace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Salida principal"))
                .andExpect(jsonPath("$.piso").value(1))
                .andExpect(jsonPath("$.latitud").value(4.78271))
                .andExpect(jsonPath("$.longitud").value(-74.04263));
    }

    @Test
    void zonaSegura_delQrALaConsultaPublica() throws Exception {
        String enlace = enlaceDelQr("/api/v1/safe-zones/" + zona + "/qr");

        assertThat(enlace).isEqualTo(BASE_QR + "/#/qr/zonas/" + zona);
        mockMvc.perform(abrirSinSesion(enlace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("QR-MP-01"))
                .andExpect(jsonPath("$.puntoEncuentro").value(true))
                .andExpect(jsonPath("$.capacidad").doesNotExist());
    }

    @Test
    void salidaInactivada_laConsultaPublicaAvisaQueNoEstaDisponible() throws Exception {
        String enlace = enlaceDelQr("/api/v1/exits/" + salida + "/qr");
        mockMvc.perform(comoAdmin(patch("/api/v1/exits/" + salida + "/deactivate")))
                .andExpect(status().isOk());

        mockMvc.perform(abrirSinSesion(enlace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.aviso").exists());
    }

    @Test
    void espacioConRutaActiva_noSePuedeInactivarYSigueDisponibleEnLaConsultaPublica() throws Exception {
        String enlace = enlaceDelQr("/api/v1/spaces/" + espacio + "/qr");
        mockMvc.perform(comoAdmin(patch("/api/v1/spaces/" + espacio + "/deactivate")))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(comoAdmin(patch("/api/v1/buildings/" + edificio + "/deactivate")))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(abrirSinSesion(enlace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void unEstudianteNoGeneraQrPeroSiPuedeConsultarloSinSesion() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/" + espacio + "/qr")
                        .header("X-User-Id", "USR-1034").header("X-User-Role", "ESTUDIANTE"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/public/spaces").param("id", espacio))
                .andExpect(status().isOk());
    }

    @Test
    void laConsultaPublicaNoPermiteEscribir() throws Exception {
        mockMvc.perform(post("/api/v1/public/spaces").param("id", espacio))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(comoAdmin(post("/api/v1/public/spaces")).param("id", espacio))
                .andExpect(status().isForbidden());
    }
}
