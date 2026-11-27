package edu.eci.dosw.campusseguro.campus.ruta.dto;

// Resultado de una generación de rutas exteriores. "interrumpida" indica que el proveedor de navegación
// dejó de responder y se cortó el proceso; lo que falte se genera al repetir la operación.
public record ResumenGeneracionResponse(int generadas, int omitidas, boolean interrumpida) {

    public static ResumenGeneracionResponse vacio() {
        return new ResumenGeneracionResponse(0, 0, false);
    }
}
