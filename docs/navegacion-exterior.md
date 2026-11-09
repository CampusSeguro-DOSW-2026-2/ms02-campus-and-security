# Navegación exterior

MS02 calcula el recorrido **peatonal exterior** (de una salida del edificio a una zona segura). El recorrido dentro del edificio no se calcula (ver ADR-003).

## Cómo está armado

- `ProveedorNavegacion` (paquete `ruta.port`) es la interfaz que usan los casos de uso. Recibe dos `Coordenada` y devuelve una `RutaCalculada` (distancia, duración, geometría y pasos).
- `OsmProveedorNavegacion` (paquete `ruta.navegacion`) es la única clase que conoce el formato de OSRM. El dominio no importa nada de OpenStreetMap.
- Para pruebas existe `FakeProveedorNavegacion` (en `src/test`): calcula una línea recta, sin red.

## Configuración

| Propiedad | Variable de entorno | Por defecto |
|---|---|---|
| `campusseguro.navigation.base-url` | `NAVIGATION_BASE_URL` | `https://routing.openstreetmap.de/routed-foot` |
| `campusseguro.navigation.profile` | `NAVIGATION_PROFILE` | `foot` |
| `campusseguro.navigation.connect-timeout` | `NAVIGATION_CONNECT_TIMEOUT` | `3s` |
| `campusseguro.navigation.read-timeout` | `NAVIGATION_READ_TIMEOUT` | `10s` |

Cambiar de servidor es solo cambiar `NAVIGATION_BASE_URL`; no hay que tocar código.

## Límites del servicio público

- El servidor demo de OSRM (`router.project-osrm.org`) solo calcula rutas en carro y no sirve para peatones.
- `routing.openstreetmap.de` tiene un servicio con perfil a pie, pero es de uso compartido y moderado. Revisa sus condiciones de uso antes de depender de él.
- Para producción conviene levantar un OSRM propio con el perfil `foot` y un extracto de OpenStreetMap de Colombia, y apuntar `NAVIGATION_BASE_URL` a ese servidor.
- Por eso la ruta calculada se guarda (ADR-003, D2): la consulta de emergencia no debe depender de internet.

## Errores

| Situación | Excepción | Respuesta HTTP |
|---|---|---|
| El proveedor no responde, da timeout o devuelve error | `NavegacionNoDisponibleException` | 503 `NAVEGACION_NO_DISPONIBLE` |
| No existe recorrido peatonal entre los puntos | `RutaNoEncontradaException` | 422 `RUTA_NO_ENCONTRADA` |

El detalle del proveedor se registra en el log y no se devuelve al cliente.

## Ruta exterior y guía de evacuación (PR 5)

**Calcular y guardar la ruta** (solo ADMIN, permiso `ms02:route:write`):

```
POST /api/v1/evacuation-routes/exterior
{ "salidaId": "<uuid>", "zonaSeguraId": "<uuid>" }
```

- La salida y la zona deben estar activas y tener coordenadas; si no, responde 422.
- Llama a `ProveedorNavegacion`, guarda distancia, duración, geometría y pasos (migración V15) y devuelve el recorrido.
- Si el par salida/zona ya tenía ruta, la recalcula en vez de crear otra (D2). Una ruta exterior no se edita con `PUT`.
- Las rutas de legado (espacio → zona, en texto) siguen igual: `ruta_evacuacion.espacio_origen_id` ahora es nullable y cada fila tiene un solo origen (espacio o salida).

**Consultar lo guardado** (cualquier rol, no llama al proveedor):

```
GET /api/v1/exits/{id}/evacuation-route
```

**Guía completa desde un espacio** (`GET /api/v1/spaces/{id}/evacuation-route`): se mantienen todos los campos anteriores y se agrega `guia` con edificio, piso, escaleras disponibles, salida, ruta exterior y una indicación en texto, por ejemplo: "Baje por la escalera X hasta el piso de salida, salga por 'Y' y siga la ruta exterior (280 m, unos 4 min) hasta la zona segura 'Z'."

- Solo se recomiendan escaleras `DISPONIBLE` y activas que conecten el piso del espacio con el piso de la salida (una escalera directa; no se encadenan escaleras).
- Si la salida está en el mismo piso del espacio no hace falta escalera.
- Entre varias salidas se elige la de recorrido exterior más corto, con ruta `DISPONIBLE` y zona de destino activa.
- Si no hay escalera disponible o ninguna salida tiene ruta, `guia.mensaje` lo explica y `guia.salida` queda en `null`.
