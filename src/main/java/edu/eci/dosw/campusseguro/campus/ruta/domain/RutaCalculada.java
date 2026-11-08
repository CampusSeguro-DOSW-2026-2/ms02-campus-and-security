package edu.eci.dosw.campusseguro.campus.ruta.domain;

import java.util.List;

// Resultado de calcular un recorrido peatonal exterior. No depende de ningún proveedor de navegación.
public record RutaCalculada(
        double distanciaMetros,
        double duracionSegundos,
        List<Coordenada> geometria,
        List<PasoRuta> pasos
) {

    public RutaCalculada {
        geometria = List.copyOf(geometria);
        pasos = List.copyOf(pasos);
    }
}
