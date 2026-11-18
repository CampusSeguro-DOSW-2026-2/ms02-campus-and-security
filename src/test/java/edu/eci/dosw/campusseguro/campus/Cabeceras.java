package edu.eci.dosw.campusseguro.campus;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Emula al API Gateway en las pruebas: traduce los roles iniciales a los permisos de MS02 segun la
 * asignacion inicial de la Matriz Rol-Permiso.
 *
 * En produccion la cabecera X-User-Permission la pone el Gateway a partir del claim permissions del
 * JWT, ya con la union de los permisos de todos los roles del usuario. MS02 nunca deriva permisos de
 * los roles: los roles son datos de MS01 y cambian en caliente.
 */
public final class Cabeceras {

    public static final String ID = "X-User-Id";
    public static final String ROL = "X-User-Role";
    public static final String PERM = "X-User-Permission";

    private static final Set<String> LECTURA = Set.of(
            "ms02:campus:read", "ms02:route:read", "ms02:preventive_info:read");

    private static final Set<String> COORDINADOR = Set.of(
            "ms02:campus:read", "ms02:route:read", "ms02:preventive_info:read", "ms02:route_status:write");

    private static final Set<String> ADMIN = Set.of(
            "ms02:campus:read", "ms02:route:read", "ms02:preventive_info:read", "ms02:building:write",
            "ms02:space:write", "ms02:safe_zone:write", "ms02:route:write", "ms02:route_status:write",
            "ms02:qr:manage");

    private static final Map<String, Set<String>> POR_ROL = Map.of(
            "ESTUDIANTE", LECTURA,
            "PROFESOR", LECTURA,
            "ADMINISTRATIVO", LECTURA,
            "BRIGADISTA", LECTURA,
            "ORGANIZADOR", LECTURA,
            "AUDITOR", LECTURA,
            "COORDINADOR", COORDINADOR,
            "ADMIN", ADMIN);

    private Cabeceras() {
    }

    /** Permisos de MS02 que la asignacion inicial concede a los roles recibidos (separados por coma). */
    public static String permisosDe(String roles) {
        Set<String> permisos = new TreeSet<>();
        for (String rol : roles.split(",")) {
            permisos.addAll(POR_ROL.getOrDefault(rol.trim(), Set.of()));
        }
        return String.join(",", new LinkedHashSet<>(permisos));
    }
}
