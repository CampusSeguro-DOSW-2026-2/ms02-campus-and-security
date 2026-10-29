# ADR-002: Gateway Identity and API Errors

- Status: Proposed
- Context: MS02 receives requests through the API Gateway and communicates with other campus services.
- Decision: The Gateway must forward X-User-Id, X-User-Role, and X-Correlation-ID.
- Decision: API errors must follow the error contract agreed with MS01.
- Decision: A safe zone represents a designated meeting point.
- Pending validation: Confirm the Gateway base route and the exact MS01 error format with the responsible teams.
