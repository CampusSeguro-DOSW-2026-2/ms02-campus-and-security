package edu.eci.dosw.campusseguro.campus.ruta.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// @EnableAsync: el listener corre en otro hilo para no hacer esperar al administrador mientras se consulta al proveedor.
@Configuration
@EnableAsync
@EnableConfigurationProperties(RutasAutoProperties.class)
public class RutasAutoConfig {
}
