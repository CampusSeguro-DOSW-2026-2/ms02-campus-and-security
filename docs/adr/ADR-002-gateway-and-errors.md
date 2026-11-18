# ADR-002: Gateway Identity and API Errors

- Status: Proposed
- Context: MS02 receives requests through the API Gateway and communicates with other campus services.
- Decision: The Gateway must forward X-User-Id, X-User-Role, and X-Correlation-ID.
- Update (2026-11-17): the Gateway also forwards X-User-Permission (the union of the user's permissions). MS02 authorizes with it against the `x-permiso` of each operation; X-User-Role is informative only.
- Decision: API errors must follow the error contract agreed with MS01.
- Decision: A safe zone represents a designated meeting point.
- Pending validation: Confirm the Gateway base route and the exact MS01 error format with the responsible teams.
