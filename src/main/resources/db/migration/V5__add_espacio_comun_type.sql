-- RF-03: se agrega el tipo «Espacio común».
ALTER TABLE espacio DROP CONSTRAINT ck_espacio_tipo;
ALTER TABLE espacio
    ADD CONSTRAINT ck_espacio_tipo CHECK (tipo IN ('SALON', 'LABORATORIO', 'AUDITORIO', 'OFICINA', 'ESPACIO_COMUN'));
