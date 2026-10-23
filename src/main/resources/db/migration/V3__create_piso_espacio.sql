CREATE TABLE piso (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edificio_id UUID         NOT NULL REFERENCES edificio (id),
    numero      INTEGER      NOT NULL,
    nombre      VARCHAR(100),
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT uk_piso_edificio_numero UNIQUE (edificio_id, numero),
    -- Permite que espacio referencie (piso_id, edificio_id) y asi garantizar que el piso es del mismo edificio
    CONSTRAINT uk_piso_id_edificio UNIQUE (id, edificio_id),
    CONSTRAINT ck_piso_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE espacio (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edificio_id UUID         NOT NULL REFERENCES edificio (id),
    piso_id     UUID         NOT NULL,
    codigo      VARCHAR(50)  NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT fk_espacio_piso FOREIGN KEY (piso_id, edificio_id) REFERENCES piso (id, edificio_id),
    CONSTRAINT uk_espacio_edificio_codigo UNIQUE (edificio_id, codigo),
    CONSTRAINT ck_espacio_tipo CHECK (tipo IN ('SALON', 'LABORATORIO', 'AUDITORIO', 'OFICINA')),
    CONSTRAINT ck_espacio_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_espacio_piso ON espacio (piso_id);
