package edu.eci.dosw.campusseguro.campus.security;

// Codigos de permiso de MS02, tomados del catalogo de la Matriz Rol-Permiso (x-permiso del swagger).
// MS02 no puede inventar codigos: el catalogo es de MS01 y el administrador solo puede asignar estos.
public final class Permisos {

    public static final String CAMPUS_READ = "ms02:campus:read";
    public static final String ROUTE_READ = "ms02:route:read";
    public static final String PREVENTIVE_INFO_READ = "ms02:preventive_info:read";
    public static final String BUILDING_WRITE = "ms02:building:write";
    public static final String SPACE_WRITE = "ms02:space:write";
    public static final String SAFE_ZONE_WRITE = "ms02:safe_zone:write";
    public static final String ROUTE_WRITE = "ms02:route:write";
    public static final String ROUTE_STATUS_WRITE = "ms02:route_status:write";
    public static final String QR_MANAGE = "ms02:qr:manage";

    private Permisos() {
    }
}
