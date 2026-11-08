package edu.eci.dosw.campusseguro.campus.ruta.navegacion;

import java.net.http.HttpClient;

import edu.eci.dosw.campusseguro.campus.ruta.port.ProveedorNavegacion;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NavegacionProperties.class)
public class NavegacionConfig {

    @Bean
    public ProveedorNavegacion proveedorNavegacion(NavegacionProperties propiedades) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(propiedades.connectTimeout())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(propiedades.readTimeout());

        RestClient restClient = RestClient.builder().requestFactory(fabrica).build();
        return new OsmProveedorNavegacion(restClient, propiedades.baseUrl(), propiedades.profile());
    }
}
