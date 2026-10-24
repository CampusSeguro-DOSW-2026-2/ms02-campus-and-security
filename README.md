# ms02-campus-and-security
MS02 - Buildings, spaces, safe zones, meeting points, and evacuation routes service.

## Requerimientos funcionales

**RF-01 · Gestionar edificios**
El administrador podrá crear, modificar e inactivar edificios.

**RF-02 · Gestionar pisos**
El administrador podrá registrar pisos asociados a un edificio.

**RF-03 · Gestionar espacios**
El administrador podrá registrar, modificar e inactivar espacios (salones, laboratorios, auditorios, oficinas y espacios comunes) asociados a un piso de un edificio. 

**RF-04 · Gestionar zonas seguras / puntos de encuentro**
El administrador podrá egistrar, modificar e inactivar zonas seguras, indicando código, nombre, ubicación, descripción y capacidad aproximada. 

**RF-05 · Gestionar rutas de evacuación**
El administrador podrá asociar espacios con rutas recomendadas y definir su estado (disponible, bloqueada, restringida).

**RF-06 · Consultar ruta de evacuación**
El usuario podrá consultar la ruta recomendada según el edificio o espacio seleccionado.

**RF-07 · Reactivar elementos del campus**
El administrador podrá reactivar un edificio, piso, espacio o zona segura que fue inactivado, conservando todos sus datos.

**RF-08 · Gestionar información preventiva**
El administrador podrá registrar, modificar e inactivar recomendaciones preventivas asociadas a un edificio o a un tipo de espacio, y al usuario consultarlas.

**RF-09 · Consultar catálogo del campus**
Los usuarios y otros servicios podrán consultar los edificios, pisos, espacios y zonas seguras del campus con sus identificadores y estado.


```
Edificio (1) ──< (N) Piso (1) ──< (N) Espacio
Edificio (1) ──< (N) ZonaSegura
Espacio (1) ──< (N) RutaEvacuacion >── (N) ZonaSegura (destino)
```

- **Edificio**: id, nombre, código, estado (ACTIVO/INACTIVO)
- **Piso**: id, edificioId, nivel, descripción
- **Espacio**: id, pisoId, tipo (SALON/LABORATORIO/AUDITORIO/OFICINA/COMUN), nombre, estado
- **ZonaSegura**: id, nombre, descripción, capacidadAproximada, edificioId (opcional, puede ser exterior), estado (DISPONIBLE/INACTIVA)
- **RutaEvacuacion**: id, espacioOrigenId, zonaSeguraDestinoId, descripción, estado (DISPONIBLE/BLOQUEADA/RESTRINGIDA), rutaAlternativaId (autorreferencia, nullable)

**Regla de dominio:** `Edificio`, `Espacio` y `ZonaSegura` no se eliminan (hard delete) si tienen una `RutaEvacuacion` con estado distinto de inactiva referenciándolos — solo se inactivan.

## Contratos API
https://editor.swagger.io/




