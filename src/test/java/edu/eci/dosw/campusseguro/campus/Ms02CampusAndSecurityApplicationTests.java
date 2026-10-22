package edu.eci.dosw.campusseguro.campus;

import edu.eci.dosw.campusseguro.campus.ruta.integration.TestGatewaysConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// TODO: quitar @Import(TestGatewaysConfig.class) cuando las Personas 1 y 2 entreguen
// las implementaciones reales de los gateways.
@SpringBootTest
@ActiveProfiles("test")
@Import(TestGatewaysConfig.class)
class Ms02CampusAndSecurityApplicationTests {

    @Test
    void contextLoads() {
    }

}
