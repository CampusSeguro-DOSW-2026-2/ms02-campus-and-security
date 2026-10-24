ALTER TABLE ruta_evacuacion ADD COLUMN motivo VARCHAR(255);

UPDATE ruta_evacuacion SET motivo = 'Motivo no registrado' WHERE estado <> 'DISPONIBLE';

ALTER TABLE ruta_evacuacion
    ADD CONSTRAINT ck_ruta_motivo
        CHECK ((estado = 'DISPONIBLE' AND motivo IS NULL) OR (estado <> 'DISPONIBLE' AND motivo IS NOT NULL));
