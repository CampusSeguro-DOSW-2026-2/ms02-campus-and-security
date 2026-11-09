package edu.eci.dosw.campusseguro.campus.dto;

import java.util.List;

import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;

public record EspacioPublicoResponse(
        String codigo,
        String nombre,
        TipoEspacio tipo,
        String edificio,
        Integer piso,
        boolean disponible,
        Evacuacion evacuacion,
        String aviso,
        boolean tieneFotografia
) {

    public record Evacuacion(
            boolean tieneRuta,
            String zonaSegura,
            String ubicacionZonaSegura,
            String recorrido,
            EstadoRuta estado,
            String motivo,
            String mensaje,
            List<Recomendacion> recomendaciones
    ) {
    }

    public record Recomendacion(String titulo, String recomendacion) {
    }
}
