package edu.eci.dosw.campusseguro.campus.dto;

import java.math.BigDecimal;
import java.util.List;

import edu.eci.dosw.campusseguro.campus.entity.TipoEspacio;
import edu.eci.dosw.campusseguro.campus.ruta.domain.EstadoRuta;

public record EspacioPublicoResponse(
        String nombre,
        TipoEspacio tipo,
        String edificio,
        Integer piso,
        boolean disponible,
        Evacuacion evacuacion,
        String aviso,
        boolean tieneFotografia,
        Guia guia
) {

    public record Guia(
            String indicacion,
            String mensaje,
            List<String> escaleras,
            String salida,
            BigDecimal salidaLatitud,
            BigDecimal salidaLongitud,
            String zonaSegura,
            BigDecimal distanciaMetros,
            BigDecimal duracionSegundos,
            List<Paso> pasos
    ) {
    }

    public record Paso(String instruccion, double distanciaMetros) {
    }

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
