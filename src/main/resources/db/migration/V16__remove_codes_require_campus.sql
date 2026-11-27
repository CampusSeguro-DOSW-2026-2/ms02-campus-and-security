-- Edificios, espacios y salidas se identifican solo por su UUID; las zonas seguras conservan su código.
-- Todo edificio pertenece a un campus (ADR-003).
UPDATE edificio
SET campus_id = COALESCE(
        (SELECT id FROM campus WHERE nombre = 'Campus principal' LIMIT 1),
        (SELECT id FROM campus ORDER BY nombre LIMIT 1))
WHERE campus_id IS NULL;

ALTER TABLE edificio ALTER COLUMN campus_id SET NOT NULL;
ALTER TABLE edificio DROP COLUMN codigo;
ALTER TABLE espacio DROP COLUMN codigo;
ALTER TABLE salida DROP COLUMN codigo;
