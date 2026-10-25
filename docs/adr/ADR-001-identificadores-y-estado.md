# ADR-001: Identifiers and Entity State

- Status: Proposed
- Context: MS02 manages campus buildings, spaces, safe zones, evacuation routes, and preventive recommendations.
- Decision: Use UUIDs as identifiers for entities exposed through the API.
- Decision: Inactivation must preserve the record instead of physically deleting it.
- Consequences: Consumers must check entity state before using a building, space, or safe zone.
- Pending validation: Confirm the identifier format and state conventions with the team.
