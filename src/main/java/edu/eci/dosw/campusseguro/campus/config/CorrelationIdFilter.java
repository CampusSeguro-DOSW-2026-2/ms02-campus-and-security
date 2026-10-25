package edu.eci.dosw.campusseguro.campus.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = valido(request.getHeader(HEADER));
        request.setAttribute(HEADER, id);
        response.setHeader(HEADER, id);
        MDC.put(MDC_KEY, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public static String de(HttpServletRequest request) {
        Object id = request.getAttribute(HEADER);
        return id != null ? id.toString() : valido(request.getHeader(HEADER));
    }

    private static String valido(String recibido) {
        if (recibido != null) {
            try {
                return UUID.fromString(recibido.trim()).toString();
            } catch (IllegalArgumentException e) {
                return UUID.randomUUID().toString();
            }
        }
        return UUID.randomUUID().toString();
    }
}
