package edu.eci.dosw.campusseguro.campus.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// Lee las cabeceras que manda el Gateway (X-User-Id y X-User-Role) y arma la identidad del usuario.
// Si falta alguna, no autentica: la peticion queda anonima y la cadena responde 401.
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader(USER_ID);
        String roleHeader = request.getHeader(USER_ROLE);

        if (userId != null && !userId.isBlank() && roleHeader != null && !roleHeader.isBlank()) {
            List<String> roles = Arrays.stream(roleHeader.split(","))
                    .map(String::trim)
                    .filter(rol -> !rol.isEmpty())
                    .map(rol -> rol.toUpperCase(Locale.ROOT))
                    .toList();
            List<SimpleGrantedAuthority> autoridades = RolPermisos.permisosDe(roles).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(userId.trim(), null, autoridades));
            SecurityContextHolder.setContext(contexto);
        }
        chain.doFilter(request, response);
    }
}
