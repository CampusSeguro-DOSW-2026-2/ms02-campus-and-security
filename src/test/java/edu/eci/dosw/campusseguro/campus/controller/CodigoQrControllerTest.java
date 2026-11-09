package edu.eci.dosw.campusseguro.campus.controller;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.exception.SalidaNoEncontradaException;
import edu.eci.dosw.campusseguro.campus.service.CodigoQrService;
import edu.eci.dosw.campusseguro.campus.service.CodigoQrService.TipoQr;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CodigoQrController.class)
class CodigoQrControllerTest {

    private static final UUID ID = uuid(30);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodigoQrService codigoQrService;

    @Test
    void espacio_devuelveLaImagenPng() throws Exception {
        when(codigoQrService.generar(TipoQr.ESPACIO, ID)).thenReturn(PNG);

        mockMvc.perform(get("/api/v1/spaces/{id}/qr", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"qr-" + ID + ".png\""))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void salida_devuelveLaImagenPng() throws Exception {
        when(codigoQrService.generar(TipoQr.SALIDA, ID)).thenReturn(PNG);

        mockMvc.perform(get("/api/v1/exits/{id}/qr", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void zonaSegura_devuelveLaImagenPng() throws Exception {
        when(codigoQrService.generar(TipoQr.ZONA_SEGURA, ID)).thenReturn(PNG);

        mockMvc.perform(get("/api/v1/safe-zones/{id}/qr", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void elementoInexistente_devuelve404ConElFormatoDeError() throws Exception {
        when(codigoQrService.generar(TipoQr.SALIDA, ID)).thenThrow(new SalidaNoEncontradaException(ID));

        mockMvc.perform(get("/api/v1/exits/{id}/qr", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void idInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}/qr", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }
}
