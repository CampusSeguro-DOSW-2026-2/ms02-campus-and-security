package edu.eci.dosw.campusseguro.campus.security;

// Codigos de permiso de MS02, segun la matriz de roles y permisos de MS01 (x-permiso del swagger).
public final class Permisos {

    public static final String CAMPUS_READ = "ms02:campus:read";
    public static final String ROUTE_READ = "ms02:route:read";
    public static final String BUILDING_WRITE = "ms02:building:write";
    public static final String SAFE_ZONE_WRITE = "ms02:safe_zone:write";
    public static final String ROUTE_WRITE = "ms02:route:write";
    public static final String ROUTE_STATUS_WRITE = "ms02:route_status:write";
    public static final String PREVENTIVE_RECOMMENDATION_WRITE = "ms02:preventive_recommendation:write";

    private Permisos() {
    }
}
