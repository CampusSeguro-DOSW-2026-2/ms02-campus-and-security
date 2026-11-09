-- ADR-003 (D1, D2): la ruta de evacuación puede salir de una salida exterior y guarda el último cálculo.
-- Las rutas existentes (espacio -> zona, en texto) se conservan intactas como rutas de legado.
ALTER TABLE ruta_evacuacion
    ALTER COLUMN espacio_origen_id DROP NOT NULL;

ALTER TABLE ruta_evacuacion
    ADD COLUMN salida_origen_id  UUID REFERENCES salida (id),
    ADD COLUMN distancia_metros  NUMERIC(10, 2),
    ADD COLUMN duracion_segundos NUMERIC(10, 2),
    ADD COLUMN geometria         TEXT,
    ADD COLUMN pasos             TEXT,
    ADD COLUMN calculada_en      TIMESTAMP WITH TIME ZONE;

-- Toda ruta tiene exactamente un origen: un espacio (legado) o una salida (exterior).
ALTER TABLE ruta_evacuacion
    ADD CONSTRAINT ck_ruta_un_origen CHECK (
        (espacio_origen_id IS NOT NULL AND salida_origen_id IS NULL)
        OR (espacio_origen_id IS NULL AND salida_origen_id IS NOT NULL)
    );

-- Una sola ruta exterior guardada por par salida/zona (se recalcula, no se duplica).
ALTER TABLE ruta_evacuacion
    ADD CONSTRAINT uk_ruta_salida_zona UNIQUE (salida_origen_id, zona_segura_destino_id);

CREATE INDEX idx_ruta_salida_origen ON ruta_evacuacion (salida_origen_id);
