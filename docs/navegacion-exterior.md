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
