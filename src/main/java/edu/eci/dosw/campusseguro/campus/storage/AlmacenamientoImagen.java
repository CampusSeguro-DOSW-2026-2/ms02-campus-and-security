package edu.eci.dosw.campusseguro.campus.storage;

/**
 * Almacenamiento físico de las fotografías del campus. La implementación se elige con
 * {@code campusseguro.imagen.storage-type}: {@code local} (disco) o {@code azure} (Azure Blob Storage).
 * Las rutas son relativas (p. ej. {@code imagenes/<uuid>.jpg}) y se guardan tal cual en la tabla {@code imagen}.
 */
public interface AlmacenamientoImagen {

    /** Guarda el contenido en la ruta indicada. No sobrescribe: si ya existe, falla. */
    void guardar(String rutaRelativa, byte[] contenido);

    /** Devuelve el contenido guardado. Responde 404 si no existe. */
    byte[] leer(String rutaRelativa);

    /** Elimina el archivo. Si no existe, no hace nada. */
    void eliminar(String rutaRelativa);
}
