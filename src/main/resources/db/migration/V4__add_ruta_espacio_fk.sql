-- La ruta ya referencia zona_segura; faltaba la referencia al espacio de origen (requiere V3).
ALTER TABLE ruta_evacuacion
    ADD CONSTRAINT fk_ruta_espacio_origen FOREIGN KEY (espacio_origen_id) REFERENCES espacio (id);
