# ms02-campus-and-security
MS02 - Buildings, spaces, safe zones, meeting points, and evacuation routes service.

## Requerimientos funcionales

**RF-01 · Gestionar edificios**
El administrador podrá crear, modificar e inactivar edificios.

**RF-02 · Gestionar pisos y espacios**
El administrador podrá registrar pisos y espacios (salones, laboratorios, auditorios, oficinas) asociados a un edificio.

**RF-03 · Gestionar zonas seguras / puntos de encuentro**
El administrador podrá registrar, modificar e inactivar zonas seguras, indicando ubicación, descripción, capacidad aproximada y estado.

**RF-04 · Gestionar rutas de evacuación**
El administrador podrá asociar espacios con rutas recomendadas y definir su estado (disponible, bloqueada, restringida).

**RF-05 · Consultar ruta de evacuación**
El usuario podrá consultar la ruta recomendada según el edificio o espacio seleccionado.

**RF-06 · Restricción de eliminación**
No se podrá eliminar un edificio, espacio o zona segura referenciado por una ruta activa; solo podrá inactivarse.

**RF-07 · Exponer información vía API**
Los cambios sobre edificios, espacios, zonas seguras y rutas deberán estar disponibles inmediatamente para MS03 y MS04 mediante API documentada.

## Historias de usuario

| Historia | RF asociados | Puntos |
|---|---|---|
| HU-1 · Consultar ruta de evacuación por ubicación | RF-05, RF-04 | 5 |
| HU-2 · CRUD de edificios, pisos y espacios | RF-01, RF-02, RF-06, RF-07 | 5 |
| HU-3 · Gestión de zonas seguras / puntos de encuentro | RF-03, RF-06, RF-07 | 5 |

## Modelo de dominio

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

| Método | Endpoint | HU / RF |
|---|---|---|
| POST | `/api/v1/buildings` | HU-2a / RF-01 |
| PUT | `/api/v1/buildings/{id}` | HU-2a / RF-01 |
| PATCH | `/api/v1/buildings/{id}/status` | HU-2a / RF-06 |
| POST | `/api/v1/buildings/{buildingId}/floors` | HU-2a / RF-02 |
| POST | `/api/v1/floors/{floorId}/spaces` | HU-2a / RF-02 |
| POST | `/api/v1/safe-zones` | HU-2b / RF-03 |
| PUT | `/api/v1/safe-zones/{id}` | HU-2b / RF-03 |
| PATCH | `/api/v1/safe-zones/{id}/status` | HU-2b / RF-06 |
| GET | `/api/v1/safe-zones?buildingId=` | HU-2b / RF-07 (consumido por MS03, MS04) |
| GET | `/api/v1/spaces/{spaceId}/evacuation-route` | HU-1 / RF-05 |
| PUT | `/api/v1/evacuation-routes/{id}` | HU-1 / RF-04 |
| PATCH | `/api/v1/evacuation-routes/{id}/status` | HU-1 / RF-04 |


