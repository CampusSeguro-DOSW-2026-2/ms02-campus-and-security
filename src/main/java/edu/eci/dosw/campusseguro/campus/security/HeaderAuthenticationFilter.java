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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Lee las cabeceras que manda el Gateway y arma la identidad del usuario:
//   X-User-Id, X-User-Role (informativa) y X-User-Permission (la que autoriza).
// MS02 no deriva permisos de los roles ni consulta a MS01: los roles son datos de MS01 y cambian en
// caliente, asi que la union efectiva llega ya calculada en X-User-Permission (Estandares §4 y §6).
// Si falta alguna cabecera o su formato es invalido, la peticion queda anonima y la cadena responde 401.
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String USER_PERMISSION = "X-User-Permission";

    private static final Pattern FORMATO_ROL = Pattern.compile("^[A-Z][A-Z0-9_]{1,29}$");
    private static final Pattern FORMATO_PERMISO = Pattern.compile("^ms\\d{2}:[a-z_]+:[a-z_]+$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader(USER_ID);
        String roleHeader = request.getHeader(USER_ROLE);
        String permissionHeader = request.getHeader(USER_PERMISSION);

        if (falta(userId) || falta(roleHeader) || falta(permissionHeader)) {
            chain.doFilter(request, response);
            return;
        }

        Set<String> roles = separar(roleHeader);
        Set<String> permisos = separar(permissionHeader);
        if (!todosCumplen(roles, FORMATO_ROL) || !todosCumplen(permisos, FORMATO_PERMISO)) {
            chain.doFilter(request, response);
            return;
        }

        List<SimpleGrantedAuthority> autoridades = permisos.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(userId.trim(), null, autoridades));
        SecurityContextHolder.setContext(contexto);
        chain.doFilter(request, response);
    }

    private static boolean falta(String valor) {
        return valor == null || valor.isBlank();
    }

    private static Set<String> separar(String cabecera) {
        return Arrays.stream(cabecera.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean todosCumplen(Set<String> valores, Pattern formato) {
        return !valores.isEmpty() && valores.stream().allMatch(valor -> formato.matcher(valor).matches());
    }
}
