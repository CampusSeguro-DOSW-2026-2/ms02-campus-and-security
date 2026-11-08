-- ADR-003: las zonas seguras ganan coordenadas, zona abierta opcional y marca de punto de encuentro.
-- Todo es nullable o tiene valor por defecto, así que las zonas existentes siguen siendo válidas.
ALTER TABLE zona_segura
    ADD COLUMN latitud            NUMERIC(9, 6),
    ADD COLUMN longitud           NUMERIC(9, 6),
    ADD COLUMN zona_abierta_id    UUID REFERENCES zona_abierta (id),
    ADD COLUMN es_punto_encuentro BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE zona_segura
    ADD CONSTRAINT ck_zona_segura_coordenadas CHECK (
        (latitud IS NULL AND longitud IS NULL)
        OR (latitud BETWEEN -90 AND 90 AND longitud BETWEEN -180 AND 180)
    );

CREATE INDEX idx_zona_segura_zona_abierta ON zona_segura (zona_abierta_id);
