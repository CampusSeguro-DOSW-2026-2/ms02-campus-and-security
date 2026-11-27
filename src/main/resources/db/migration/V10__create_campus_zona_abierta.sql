-- ADR-003: campus y zonas abiertas. Los edificios existentes quedan ligados a un campus por defecto.
CREATE TABLE campus (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT ck_campus_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE zona_abierta (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campus_id UUID         NOT NULL REFERENCES campus (id),
    nombre    VARCHAR(100) NOT NULL,
    tipo      VARCHAR(20)  NOT NULL,
    latitud   NUMERIC(9, 6),
    longitud  NUMERIC(9, 6),
    estado    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT ck_zona_abierta_tipo CHECK (tipo IN ('ZONA_VERDE', 'PLAZOLETA', 'CANCHA', 'PARQUEADERO', 'OTRA')),
    CONSTRAINT ck_zona_abierta_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_zona_abierta_coordenadas CHECK (
        (latitud IS NULL AND longitud IS NULL)
        OR (latitud BETWEEN -90 AND 90 AND longitud BETWEEN -180 AND 180)
    )
);

CREATE INDEX idx_zona_abierta_campus ON zona_abierta (campus_id);

-- Nullable al inicio: los edificios nuevos pueden registrarse sin campus.
ALTER TABLE edificio ADD COLUMN campus_id UUID REFERENCES campus (id);
CREATE INDEX idx_edificio_campus ON edificio (campus_id);

INSERT INTO campus (nombre, descripcion)
VALUES ('Campus principal', 'Campus creado por defecto durante la migración');

UPDATE edificio SET campus_id = (SELECT id FROM campus WHERE nombre = 'Campus principal');
