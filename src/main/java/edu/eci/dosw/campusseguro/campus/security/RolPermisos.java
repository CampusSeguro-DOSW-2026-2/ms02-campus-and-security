package edu.eci.dosw.campusseguro.campus.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Que permisos de MS02 tiene cada rol (x-roles del swagger). Un usuario con varios roles tiene la union.
public final class RolPermisos {

    private static final Set<String> LECTURA = Set.of(Permisos.CAMPUS_READ, Permisos.ROUTE_READ);

    private static final Map<String, Set<String>> POR_ROL = Map.of(
            "ESTUDIANTE", LECTURA,
            "PROFESOR", LECTURA,
            "ADMINISTRATIVO", LECTURA,
            "BRIGADISTA", LECTURA,
            "ORGANIZADOR", LECTURA,
            "AUDITOR", LECTURA,
            "COORDINADOR", Set.of(Permisos.CAMPUS_READ, Permisos.ROUTE_READ, Permisos.ROUTE_STATUS_WRITE),
            "ADMIN", Set.of(Permisos.CAMPUS_READ, Permisos.ROUTE_READ, Permisos.BUILDING_WRITE,
                    Permisos.SAFE_ZONE_WRITE, Permisos.ROUTE_WRITE, Permisos.ROUTE_STATUS_WRITE));

    private RolPermisos() {
    }

    public static Set<String> permisosDe(Collection<String> roles) {
        Set<String> permisos = new HashSet<>();
        for (String rol : roles) {
            permisos.addAll(POR_ROL.getOrDefault(rol, Set.of()));
        }
        return permisos;
    }
}
