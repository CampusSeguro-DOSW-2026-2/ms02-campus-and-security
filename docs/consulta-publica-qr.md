# Consulta pública de los QR

Al escanear el QR de un espacio, una salida o una zona segura en una emergencia, la persona ve la información del lugar **sin iniciar sesión** (ADR-003, D7). Crear, modificar y generar QR sigue protegido para ADMIN.

## Enlaces

El QR solo contiene un enlace, sin datos del elemento. Se genera con el UUID, no hay otro código.

| Elemento | Enlace del QR (y endpoint público) | QR administrativo (PNG) |
|---|---|---|
| Espacio | `GET /api/v1/public/spaces?id={uuid}` | `GET /api/v1/spaces/{id}/qr` |
| Salida | `GET /api/v1/public/exits?id={uuid}` | `GET /api/v1/exits/{id}/qr` |
| Zona segura | `GET /api/v1/public/safe-zones?id={uuid}` | `GET /api/v1/safe-zones/{id}/qr` |

El enlace empieza por `QR_BASE_URL` (por defecto `http://localhost:8080`, el Gateway).

El id va como parámetro (`?id=`) y no en la ruta porque el Gateway solo acepta rutas públicas **exactas**, sin comodines (`gateway.access.public-routes`). Así bastan tres entradas fijas.

## Qué devuelve

Solo lo necesario para orientarse; sin ids internos, capacidad ni datos de administración.

- **Espacio:** código, nombre, tipo, edificio, número de piso, si está disponible y la evacuación recomendada (zona segura, ubicación, recorrido, estado, motivo, aviso y recomendaciones preventivas).
- **Salida:** código, nombre, edificio, piso y coordenadas.
- **Zona segura:** código, nombre, ubicación, si es punto de encuentro y coordenadas.

Si el elemento, su piso o su edificio está inactivo, responde `disponible: false` con un aviso para seguir las indicaciones del personal. Si el id no existe, `404 NO_ENCONTRADO`.

## Seguridad

- MS02 (`SecurityConfig`): `GET /api/v1/public/**` es `permitAll`; cualquier otro método sobre esa ruta es `denyAll` (401 sin sesión, 403 con sesión).
- Gateway (`docker-compose.gw.yml`): las tres rutas se declaran en `GATEWAY_ACCESS_PUBLICROUTES_*`. Esa lista reemplaza la de la imagen, por eso se repiten las rutas públicas de MS01. `/api/v1/public/**` está en `SQUAD_PATHS`.
- `ContratoPermisosTest` verifica que las operaciones públicas son solo GET, no declaran permiso y responden sin cabeceras.

## Límite de peticiones

MS02 no limita la cantidad de peticiones y el Gateway hoy tampoco lo hace. Mientras tanto, el riesgo es bajo: la consulta es de solo lectura, devuelve datos mínimos y los UUID no se pueden adivinar ni recorrer en orden. Lo recomendado es que el Gateway aplique un límite por IP a las rutas públicas; queda pendiente con el equipo del Gateway.
