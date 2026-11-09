package edu.eci.dosw.campusseguro.campus.ruta.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import edu.eci.dosw.campusseguro.campus.ruta.domain.Coordenada;
import edu.eci.dosw.campusseguro.campus.ruta.domain.PasoRuta;

// Guarda la geometría y los pasos de la ruta exterior como texto plano, para leerlos sin internet (D2).
// Geometría: "lat,lng;lat,lng". Pasos: una línea por paso, "distancia|duración|instrucción".
final class CodificadorRutaExterior {

    private CodificadorRutaExterior() {
    }

    static String codificarGeometria(List<Coordenada> geometria) {
        StringBuilder sb = new StringBuilder();
        for (Coordenada c : geometria) {
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(c.latitud().toPlainString()).append(',').append(c.longitud().toPlainString());
        }
        return sb.toString();
    }

    static List<Coordenada> decodificarGeometria(String texto) {
        List<Coordenada> resultado = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return resultado;
        }
        for (String punto : texto.split(";")) {
            String[] partes = punto.split(",");
            resultado.add(new Coordenada(new BigDecimal(partes[0]), new BigDecimal(partes[1])));
        }
        return resultado;
    }

    static String codificarPasos(List<PasoRuta> pasos) {
        StringBuilder sb = new StringBuilder();
        for (PasoRuta p : pasos) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            String instruccion = p.instruccion() == null ? "" : p.instruccion().replaceAll("[\\r\\n]+", " ");
            sb.append(p.distanciaMetros()).append('|').append(p.duracionSegundos()).append('|').append(instruccion);
        }
        return sb.toString();
    }

    static List<PasoRuta> decodificarPasos(String texto) {
        List<PasoRuta> resultado = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return resultado;
        }
        for (String linea : texto.split("\n")) {
            String[] partes = linea.split("\\|", 3);
            resultado.add(new PasoRuta(partes.length > 2 ? partes[2] : "",
                    Double.parseDouble(partes[0]), Double.parseDouble(partes[1])));
        }
        return resultado;
    }
}
