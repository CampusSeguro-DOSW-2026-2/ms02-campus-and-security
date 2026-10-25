package edu.eci.dosw.campusseguro.campus.security;

import edu.eci.dosw.campusseguro.campus.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Responde 401 y 403 con el mismo formato de error del resto del servicio (ErrorResponse).
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) throws IOException {
        escribir(request, response, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO",
                "Faltan las cabeceras de identidad.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        escribir(request, response, HttpServletResponse.SC_FORBIDDEN, "PROHIBIDO",
                "No tiene permiso para realizar esta operación.");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response, int status,
                          String codigo, String mensaje) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\"status\":").append(status)
                .append(",\"codigo\":\"").append(codigo).append('"')
                .append(",\"mensaje\":\"").append(mensaje).append('"')
                .append(",\"errores\":[]");
        json.append(",\"correlation_id\":\"").append(escapar(CorrelationIdFilter.de(request))).append('"');
        json.append('}');

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.toString());
    }

    private String escapar(String valor) {
        StringBuilder sb = new StringBuilder();
        for (char c : valor.toCharArray()) {
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
