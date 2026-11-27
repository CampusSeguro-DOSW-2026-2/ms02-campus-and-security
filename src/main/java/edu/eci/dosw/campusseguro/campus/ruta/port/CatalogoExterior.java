package edu.eci.dosw.campusseguro.campus.ruta.port;

import java.util.List;

import edu.eci.dosw.campusseguro.campus.ruta.port.SalidaGateway.SalidaInfo;
import edu.eci.dosw.campusseguro.campus.ruta.port.ZonaSeguraGateway.ZonaInfo;

// Puerto para generar rutas exteriores en bloque: lista de una vez lo que puede ser origen (salidas)
// y destino (zonas seguras), en vez de consultar elemento por elemento.
public interface CatalogoExterior {

    // Salidas activas, con su piso y su edificio activos.
    List<SalidaInfo> salidasDisponibles();

    // Zonas seguras activas que tienen latitud y longitud.
    List<ZonaInfo> zonasActivasConCoordenadas();
}
