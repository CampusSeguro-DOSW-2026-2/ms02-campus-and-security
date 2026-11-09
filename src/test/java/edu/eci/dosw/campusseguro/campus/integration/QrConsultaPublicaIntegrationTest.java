package edu.eci.dosw.campusseguro.campus.integration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.jayway.jsonpath.JsonPath;
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

    private static final String BASE_QR = "http://localhost:8080";

    @Autowired
    private MockMvc mockMvc;

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

    private static MockHttpServletRequestBuilder abrirSinSesion(String enlace) {
        URI uri = URI.create(enlace);
        return get(uri.getPath() + "?" + uri.getQuery());
    }

    @BeforeEach
    void registrarElCampus() throws Exception {
        edificio = crear("/api/v1/buildings", "{\"codigo\":\"QR-G\",\"nombre\":\"Bloque G\"}");
        piso1 = crear("/api/v1/buildings/" + edificio + "/floors", "{\"numero\":1,\"nombre\":\"Primer piso\"}");
        piso2 = crear("/api/v1/buildings/" + edificio + "/floors", "{\"numero\":2}");
        espacio = crear("/api/v1/floors/" + piso2 + "/spaces",
                "{\"codigo\":\"G-201\",\"nombre\":\"Salón 201\",\"tipo\":\"SALON\"}");
        salida = crear("/api/v1/buildings/" + edificio + "/exits", "{\"pisoId\":\"" + piso1 + "\",\"codigo\":\"G-S1\","
                + "\"nombre\":\"Salida principal\",\"latitud\":4.78271,\"longitud\":-74.04263}");
        zona = crear("/api/v1/safe-zones", "{\"codigo\":\"QR-MP-01\",\"nombre\":\"Plazoleta H\","
                + "\"ubicacion\":\"Frente al bloque H\",\"descripcion\":\"Punto de encuentro\",\"capacidad\":300,"
                + "\"latitud\":4.7831,\"longitud\":-74.0431}");
        crear("/api/v1/evacuation-routes", "{\"espacioOrigenId\":\"" + espacio + "\",\"zonaSeguraDestinoId\":\"" + zona
                + "\",\"descripcion\":\"Baje por la escalera norte y salga por la puerta principal.\","
                + "\"estado\":\"DISPONIBLE\"}");
    }

    @Test
    void espacio_delQrALaConsultaPublica() throws Exception {
        String enlace = enlaceDelQr("/api/v1/spaces/" + espacio + "/qr");

        assertThat(enlace).isEqualTo(BASE_QR + "/api/v1/public/spaces?id=" + espacio);
        mockMvc.perform(abrirSinSesion(enlace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Salón 201"))
                .andExpect(jsonPath("$.edificio").value("Bloque G"))
                .andExpect(jsonPath("$.piso").value(2))
                .andExpect(jsonPath("$.disponible").value(true))
                .andExpect(jsonPath("$.evacuacion.zonaSegura").value("Plazoleta H"))
                .andExpect(jsonPath("$.evacuacion.recorrido").value("Baje por la escalera norte y salga por la puerta principal."))
                .andExpect(jsonPath("$.evacuacion.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.edificioId").doesNotExist());
    }

    @Test
    void salida_delQrALaConsultaPublica() throws Exception {
        String enlace = enlaceDelQr("/api/v1/exits/" + salida + "/qr");

        assertThat(enlace).isEqualTo(BASE_QR + "/api/v1/public/exits?id=" + salida);
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

        assertThat(enlace).isEqualTo(BASE_QR + "/api/v1/public/safe-zones?id=" + zona);
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
