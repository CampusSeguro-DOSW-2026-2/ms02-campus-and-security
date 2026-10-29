package edu.eci.dosw.campusseguro.campus.ruta.integration;

import java.util.UUID;

import edu.eci.dosw.campusseguro.campus.ruta.port.EdificioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway;
import edu.eci.dosw.campusseguro.campus.ruta.port.EspacioGateway.EspacioInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

// Gateways falsos solo para RutaIntegrationTest. Al ser @TestConfiguration,
// Spring no los escanea en los tests de las Personas 1 y 2: hay que importarlos con @Import.
@TestConfiguration
public class TestGatewaysConfig {

    @Bean
    EnMemoriaCampus enMemoriaCampus() {
        return new EnMemoriaCampus();
    }

    @Bean
    @Primary
    EdificioGateway edificioGatewayDePrueba(EnMemoriaCampus datos) {
        return datos::buscarEdificio;
    }

    @Bean
    @Primary
    ZonaSeguraGateway zonaGatewayDePrueba(EnMemoriaCampus datos) {
        return datos::buscarZona;
    }

    @Bean
    @Primary
    EspacioGateway espacioGatewayDePrueba(EnMemoriaCampus datos) {
        return new EspacioGateway() {
            @Override
            public Optional<EspacioInfo> buscarPorId(UUID id) {
                return datos.buscarEspacio(id);
            }

            @Override
            public List<EspacioInfo> listarPorEdificio(UUID edificioId) {
                return datos.espaciosDe(edificioId);
            }
        };
    }
}
