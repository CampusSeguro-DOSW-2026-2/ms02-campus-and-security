# ADR-003: Target Model, Design Decisions and Migration Plan

- Status: Proposed
- Date: 2026-11-07
- Context: The MS02 design was corrected. Evacuation now has two independent parts: going down inside the building (floor, stairs, exit) and walking outside (exit to safe zone, computed with OpenStreetMap). The current model only has Edificio, Piso, Espacio, ZonaSegura and RutaEvacuacion (space to safe zone, described as text). Flyway is at V9.
- Scope: MS02 manages campus infrastructure and evacuation information. Attendance and evacuee control belong to MS04 and are out of scope.

## Decisions

### D1: Existing routes are kept (legacy)
`ruta_evacuacion` keeps all its rows. `salida_origen_id` is added as nullable and `espacio_origen_id` becomes nullable. Old space-based routes keep working and are treated as legacy. No data is deleted.

### D2: Exterior route is stored
The last computed result (distance, duration, simplified geometry) is saved per exit/safe-zone pair and recomputed when coordinates change. Emergency queries do not depend on the external routing service being online.

### D3: Photos are required, enforced in two steps
An element is created first and the photo is uploaded second. Responses expose `tieneFotografia` and there is a listing of elements without photos. Existing records are not blocked. To be confirmed with the instructor: if creation must be blocked without a photo, M3 and M4 change.

### D4: Navigation behind a port
`ProveedorNavegacion` (in `ruta/port`) hides the provider. First implementation: OpenStreetMap, pedestrian profile. A fake implementation is used in tests. URL and timeout are configuration. The public OSRM server is for demos only.

### D5: Image storage outside PostgreSQL
Files go to a configurable directory (`campusseguro.storage.path`) mounted as a Docker volume, behind an `AlmacenamientoArchivos` interface. The database only stores reference, name, type, size and associated element.

### D6: Existing permissions are reused
No new permissions, so MS01's matrix does not change. `ms02:building:write` covers campus, open zones, stairs, exits and images of buildings, spaces and exits. `ms02:safe_zone:write` covers safe zones and their images. Reads use `ms02:campus:read`.

### D7: Public read-only endpoint for QR
`GET /api/v1/public/**` is `permitAll` in `SecurityConfig` and must be exposed by the Gateway without a session. Any other method on that path is rejected. Public DTOs are minimal and contain no sensitive data.

## Other rules
- `PuntoEncuentro` is not a separate entity: it is a `ZonaSegura` with `es_punto_encuentro = true`.
- Open zones are not safe zones unless the administrator creates the safe zone.
- Stairs are vertical connections, never exits. They have availability (`DISPONIBLE`, `BLOQUEADA`, `FUERA_DE_SERVICIO`) and no coordinates.
- Only exterior elements have coordinates: exits, safe zones and open zones that take part in navigation.
- UUIDs are the only identifiers. QR codes are generated from the UUID; no extra codes.
- Inactivation preserves records (ADR-001). No hard deletes.

## Migration plan

Versions are reserved so parallel PRs do not collide.

| Version | PR | Change | Effect on existing data |
|---|---|---|---|
| V10 | P2 | Create `campus` and `zona_abierta`. Add `edificio.campus_id` (nullable) | A default campus is inserted and all existing buildings are linked to it |
| V11 | M2 | Create `escalera` and `escalera_piso` | New tables, no effect |
| V12 | N2 | Create `salida` (building, floor, latitude, longitude, state) with composite FK (piso, edificio) | New table, no effect |
| V13 | P3 | Add `latitud`, `longitud`, `zona_abierta_id`, `es_punto_encuentro` to `zona_segura` | Columns nullable; `es_punto_encuentro` defaults to true. Safe zones without coordinates cannot be a route destination until completed |
| V14 | M3 | Create `imagen` | New table, no effect |
| V15 | P5 | `ruta_evacuacion`: add `salida_origen_id`, make `espacio_origen_id` nullable, add distance, duration and geometry columns, add check that at least one origin is present | All current rows stay valid as legacy routes |

Order: migrations must be merged in version order (V10, V11, V12, V13, V14, V15). Flyway refuses to start on a database that already has a newer version applied when an older one arrives later.

### Rules for every migration
1. New columns that touch existing tables are nullable or have a default.
2. Backfill inside the same migration when a value is needed.
3. Never edit an already merged migration; fix with a new version.
4. Check the migration against a database with data from V9 before the PR.

### Pending data work after the migrations
- Complete coordinates for existing safe zones.
- Register exits with coordinates for each building.
- Upload at least one photo for each building, space, exit and safe zone.

## Consequences
- Consumers of `RutaConsultadaResponse` are not broken: fields are only added.
- Responsibilities are split across PRs P1 to P5, N1 to N5 and M1 to M5 (see the detailed plan).
- Pending confirmation: D3 with the instructor, and the public QR route with the Gateway owner.
