-- ADR-003: salidas del edificio al exterior. Solo ellas y las zonas exteriores llevan coordenadas.
CREATE TABLE salida (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edificio_id UUID          NOT NULL REFERENCES edificio (id),
    piso_id     UUID          NOT NULL,
    codigo      VARCHAR(50)   NOT NULL,
    nombre      VARCHAR(100)  NOT NULL,
    latitud     NUMERIC(9, 6) NOT NULL,
    longitud    NUMERIC(9, 6) NOT NULL,
    estado      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT fk_salida_piso FOREIGN KEY (piso_id, edificio_id) REFERENCES piso (id, edificio_id),
    CONSTRAINT uk_salida_edificio_codigo UNIQUE (edificio_id, codigo),
    CONSTRAINT ck_salida_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_salida_coordenadas CHECK (latitud BETWEEN -90 AND 90 AND longitud BETWEEN -180 AND 180)
);

CREATE INDEX idx_salida_piso ON salida (piso_id);
