CREATE TABLE escalera (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    edificio_id     UUID         NOT NULL REFERENCES edificio (id),
    codigo          VARCHAR(30)  NOT NULL UNIQUE,
    nombre          VARCHAR(100) NOT NULL,
    disponibilidad  VARCHAR(25)  NOT NULL DEFAULT 'DISPONIBLE',
    motivo          VARCHAR(255),
    estado          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT ck_escalera_disponibilidad
        CHECK (disponibilidad IN ('DISPONIBLE', 'BLOQUEADA', 'FUERA_DE_SERVICIO')),
    CONSTRAINT ck_escalera_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_escalera_motivo
        CHECK (disponibilidad = 'DISPONIBLE' OR motivo IS NOT NULL)
);

CREATE TABLE escalera_piso (
    escalera_id UUID NOT NULL REFERENCES escalera (id) ON DELETE CASCADE,
    piso_id     UUID NOT NULL REFERENCES piso (id),
    CONSTRAINT pk_escalera_piso PRIMARY KEY (escalera_id, piso_id)
);

CREATE INDEX idx_escalera_edificio ON escalera (edificio_id);
CREATE INDEX idx_escalera_piso_piso ON escalera_piso (piso_id);
