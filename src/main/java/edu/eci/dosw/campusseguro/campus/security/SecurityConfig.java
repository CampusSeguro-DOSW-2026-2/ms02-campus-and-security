package edu.eci.dosw.campusseguro.campus.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// La identidad llega del Gateway en X-User-Id y X-User-Role. MS02 no valida tokens: autoriza por permiso.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ManejadorErroresSeguridad manejadorErrores = new ManejadorErroresSeguridad();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()

                        // Rutas de evacuacion y su consulta
                        .requestMatchers(HttpMethod.GET, "/api/v1/buildings/*/evacuation-routes",
                                "/api/v1/spaces/*/evacuation-route").hasAuthority(Permisos.ROUTE_READ)
                        .requestMatchers(HttpMethod.GET, "/api/v1/evacuation-routes",
                                "/api/v1/evacuation-routes/*").hasAuthority(Permisos.ROUTE_READ)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/evacuation-routes/*/status")
                        .hasAuthority(Permisos.ROUTE_STATUS_WRITE)
                        .requestMatchers("/api/v1/evacuation-routes", "/api/v1/evacuation-routes/**")
                        .hasAuthority(Permisos.ROUTE_WRITE)

                        .requestMatchers(HttpMethod.GET, "/api/v1/floors/*/spaces", "/api/v1/buildings/*/spaces",
                                "/api/v1/spaces/*").hasAuthority(Permisos.CAMPUS_READ)
                        .requestMatchers("/api/v1/floors/*/spaces", "/api/v1/spaces/**")
                        .hasAuthority(Permisos.BUILDING_WRITE)

                        // Pisos (RF-02): van antes de /buildings/** para que su lectura no exija BUILDING_WRITE.
                        // Gestionar pisos es parte de gestionar edificios (HU-2a), por eso usa el mismo permiso.
                        .requestMatchers(HttpMethod.GET, "/api/v1/buildings/*/floors", "/api/v1/floors/*")
                        .hasAuthority(Permisos.CAMPUS_READ)
                        .requestMatchers("/api/v1/buildings/*/floors", "/api/v1/floors/**")
                        .hasAuthority(Permisos.BUILDING_WRITE)

                        // Edificios y zonas seguras: lectura para cualquier rol, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/buildings", "/api/v1/buildings/*",
                                "/api/v1/safe-zones", "/api/v1/safe-zones/*").hasAuthority(Permisos.CAMPUS_READ)
                        .requestMatchers("/api/v1/buildings/**").hasAuthority(Permisos.BUILDING_WRITE)
                        .requestMatchers("/api/v1/safe-zones/**").hasAuthority(Permisos.SAFE_ZONE_WRITE)

                        // Cualquier otro endpoint exige al menos estar autenticado.
                        // Quien lo agregue debe declarar aqui su permiso.
                        .anyRequest().authenticated());

        return http.build();
    }
}
