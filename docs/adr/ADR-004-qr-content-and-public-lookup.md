# ADR-004: QR Content and Public Lookup

- Status: Proposed
- Date: 2026-11-08
- Context: ADR-003 (D7) requires QR codes for spaces, exits and safe zones, generated from the UUID, and a public read-only lookup for emergencies. When implementing it (N3 and N4) we found that the API Gateway only accepts exact public routes, without wildcards (`gateway.access.public-routes`), so `/api/v1/public/**` cannot be exposed as written in ADR-003.

## Decisions

### What the QR contains
Only a lookup link, never data about the element:

`{QR_BASE_URL}/api/v1/public/{spaces|exits|safe-zones}?id={uuid}`

- The UUID is the only identifier. No extra codes are created or stored.
- The PNG is generated on request (`GET /api/v1/{spaces|exits|safe-zones}/{id}/qr`); nothing is saved.
- `QR_BASE_URL` is configuration and points to the Gateway, never to MS02 directly.

### Why the id is a query parameter
Each QR has a different UUID, so a path parameter would need a wildcard route in the Gateway. With `?id=` there are only three exact public routes, which the Gateway already supports. They are declared in `docker-compose.gw.yml` (`GATEWAY_ACCESS_PUBLICROUTES_*`).

### Public responses are minimal
The public endpoints return what a person needs to orient themselves: code, name, building name, floor number, availability, coordinates for exits and safe zones, and the recommended evacuation for spaces. They never return internal ids, capacity or administrative fields. If the element, its floor or its building is inactive, the response says so (`disponible: false`) with a notice instead of failing.

### Access
- `GET /api/v1/public/**` is `permitAll` in MS02; any other method on that path is `denyAll`.
- Generating a QR image is an administrative action: `ms02:building:write` for spaces and exits, `ms02:safe_zone:write` for safe zones (ADR-003, D6).

## Alternatives considered

| Option | Why not |
|---|---|
| `/api/v1/public/{type}/{id}` with a wildcard public route | Requires changing the Gateway, which deliberately only accepts exact routes |
| One endpoint `/api/v1/public/lookup?type=&id=` | A single operation returning three different shapes is harder to document and consume |
| Put the element's data inside the QR | Printed data gets outdated (e.g. a blocked route) and could expose information; a link always shows the current state |
| Store the QR images | They are cheap to regenerate from the UUID; storing them adds files to keep in sync |

## Consequences
- **Printed QR codes depend on the link format and on `QR_BASE_URL`.** Both must be final (public domain of the Gateway) before printing; changing either one breaks every printed code.
- The Gateway's public route list replaces the one in its image, so MS01's public routes are repeated in `docker-compose.gw.yml`. Any new public route must be added there.
- There is no rate limiting yet, neither in MS02 nor in the Gateway. The risk is low (read-only, minimal data, UUIDs cannot be guessed or enumerated), but a per-IP limit on the public routes should be added in the Gateway.
- Photos (M3, M4) and the full evacuation guide (P5) will be added to the public space response as new fields, without changing the existing ones.
- Details and examples: `docs/consulta-publica-qr.md`.
