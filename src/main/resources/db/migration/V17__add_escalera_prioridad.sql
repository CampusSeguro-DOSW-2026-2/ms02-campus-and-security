-- Prioridad de la escalera dentro del edificio: 1 es la principal. La guia de evacuacion recomienda
-- primero las de menor numero y deja las demas como alternas.
ALTER TABLE escalera ADD COLUMN prioridad INTEGER NOT NULL DEFAULT 1;
ALTER TABLE escalera ADD CONSTRAINT ck_escalera_prioridad CHECK (prioridad >= 1);
