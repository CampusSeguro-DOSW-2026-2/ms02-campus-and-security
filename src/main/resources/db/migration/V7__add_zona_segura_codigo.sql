ALTER TABLE zona_segura ADD COLUMN codigo VARCHAR(50);

UPDATE zona_segura
   SET codigo = 'MP-' || UPPER(SUBSTRING(REPLACE(id::text, '-', ''), 1, 6))
 WHERE codigo IS NULL;

ALTER TABLE zona_segura ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE zona_segura ADD CONSTRAINT uk_zona_segura_codigo UNIQUE (codigo);