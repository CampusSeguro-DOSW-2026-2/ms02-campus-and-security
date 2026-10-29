CREATE TABLE ruta_evacuacion (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    espacio_origen_id       UUID        NOT NULL,
    zona_segura_destino_id  UUID        NOT NULL REFERENCES zona_segura (id),
    ruta_alternativa_id     UUID        REFERENCES ruta_evacuacion (id),
    descripcion             VARCHAR(1000) NOT NULL,
    estado                  VARCHAR(20)   NOT NULL DEFAULT 'DISPONIBLE',
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_ruta_espacio_origen UNIQUE (espacio_origen_id),
    CONSTRAINT ck_ruta_estado CHECK (estado IN ('DISPONIBLE', 'BLOQUEADA', 'RESTRINGIDA'))
);

CREATE INDEX idx_ruta_zona_destino ON ruta_evacuacion (zona_segura_destino_id);
