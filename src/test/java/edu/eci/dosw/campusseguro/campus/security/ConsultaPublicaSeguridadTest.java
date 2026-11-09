package edu.eci.dosw.campusseguro.campus.security;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.controller.ConsultaPublicaController;
import edu.eci.dosw.campusseguro.campus.dto.EspacioPublicoResponse;
import edu.eci.dosw.campusseguro.campus.dto.SalidaPublicaResponse;
import edu.eci.dosw.campusseguro.campus.dto.ZonaSeguraPublicaResponse;
import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.exception.EspacioNoEncontradoException;
import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;
import edu.eci.dosw.campusseguro.campus.service.ConsultaPublicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static edu.eci.dosw.campusseguro.campus.Ids.uuid;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultaPublicaController.class)
@Import(SecurityConfig.class)
class ConsultaPublicaSeguridadTest {

    private static final UUID ID = uuid(10);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultaPublicaService consultaPublicaService;

    private static EspacioPublicoResponse espacio() {
        return new EspacioPublicoResponse("G-201", "Salón 201", TipoEspacio.SALON, "Bloque G", 2, true,
                new EspacioPublicoResponse.Evacuacion(true, "Plazoleta H", "Frente al bloque H",
                        "Baje por la escalera norte.", EstadoRuta.DISPONIBLE, null, null,
                        List.of(new EspacioPublicoResponse.Recomendacion("Escaleras", "Use la escalera norte."))),
                null, false,
                new EspacioPublicoResponse.Guia("Baje por la escalera norte y salga por 'Salida principal'.", null,
                        List.of("Escalera norte"), "Salida principal", new BigDecimal("4.782710"),
                        new BigDecimal("-74.042630"), "Plazoleta H", new BigDecimal("184.20"), new BigDecimal("147.40"),
                        List.of(new EspacioPublicoResponse.Paso("Inicia el recorrido", 39.9))));
    }

    @Test
    void espacio_sinCabeceras_respondeConLosDatosMinimos() throws Exception {
        when(consultaPublicaService.espacio(ID)).thenReturn(espacio());

        mockMvc.perform(get("/api/v1/public/spaces").param("id", ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Salón 201"))
                .andExpect(jsonPath("$.edificio").value("Bloque G"))
                .andExpect(jsonPath("$.piso").value(2))
                .andExpect(jsonPath("$.evacuacion.zonaSegura").value("Plazoleta H"))
                .andExpect(jsonPath("$.guia.indicacion").value("Baje por la escalera norte y salga por 'Salida principal'."))
                .andExpect(jsonPath("$.guia.escaleras[0]").value("Escalera norte"))
                .andExpect(jsonPath("$.guia.pasos[0].instruccion").value("Inicia el recorrido"))
                .andExpect(jsonPath("$.guia.salidaId").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.edificioId").doesNotExist())
                .andExpect(jsonPath("$.pisoId").doesNotExist())
                .andExpect(jsonPath("$.evacuacion.zonaSeguraId").doesNotExist())
                .andExpect(jsonPath("$.estado").doesNotExist());
    }

    @Test
    void salida_sinCabeceras_respondeSinIdsInternos() throws Exception {
        when(consultaPublicaService.salida(ID)).thenReturn(new SalidaPublicaResponse("G-S1", "Salida principal",
                "Bloque G", 1, new BigDecimal("4.782710"), new BigDecimal("-74.042630"), true, null, false));

        mockMvc.perform(get("/api/v1/public/exits").param("id", ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitud").value(4.78271))
                .andExpect(jsonPath("$.edificioId").doesNotExist());
    }

    @Test
    void zonaSegura_sinCabeceras_respondeSinCapacidadNiIds() throws Exception {
        when(consultaPublicaService.zonaSegura(ID)).thenReturn(new ZonaSeguraPublicaResponse("MP-H-01", "Plazoleta H",
                "Frente al bloque H", true, new BigDecimal("4.783100"), new BigDecimal("-74.043100"), true, null, false));

        mockMvc.perform(get("/api/v1/public/safe-zones").param("id", ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntoEncuentro").value(true))
                .andExpect(jsonPath("$.capacidad").doesNotExist())
                .andExpect(jsonPath("$.zonaAbiertaId").doesNotExist());
    }

    @Test
    void elementoInexistente_devuelve404() throws Exception {
        when(consultaPublicaService.espacio(ID)).thenThrow(new EspacioNoEncontradoException(ID));

        mockMvc.perform(get("/api/v1/public/spaces").param("id", ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void sinId_oConIdInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/public/spaces"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        mockMvc.perform(get("/api/v1/public/spaces").param("id", "no-es-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otrosMetodos_sinSesion_seRechazanCon401() throws Exception {
        mockMvc.perform(post("/api/v1/public/spaces").param("id", ID.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/public/exits").param("id", ID.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/public/safe-zones").param("id", ID.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otrosMetodos_inclusoComoAdmin_seRechazanCon403() throws Exception {
        mockMvc.perform(post("/api/v1/public/spaces").param("id", ID.toString())
                        .header("X-User-Id", "USR-9").header("X-User-Role", "ADMIN"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/public/spaces").param("id", ID.toString())
                        .header("X-User-Id", "USR-9").header("X-User-Role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laConsultaPublicaNoAbreOtrasRutas() throws Exception {
        mockMvc.perform(get("/api/v1/spaces/{id}", ID))
                .andExpect(status().isUnauthorized());
    }
}
