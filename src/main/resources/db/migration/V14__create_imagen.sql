CREATE TABLE imagen (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_elemento   VARCHAR(30)  NOT NULL,
    elemento_id     UUID         NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    tipo_contenido  VARCHAR(50)  NOT NULL,
    tamanio_bytes   BIGINT       NOT NULL,
    ruta_archivo    VARCHAR(500) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_imagen_tipo_elemento
        CHECK (tipo_elemento IN ('EDIFICIO', 'ESPACIO', 'SALIDA', 'ZONA_SEGURA')),

    CONSTRAINT ck_imagen_tipo_contenido
        CHECK (tipo_contenido IN ('image/jpeg', 'image/png', 'image/webp')),

    CONSTRAINT ck_imagen_tamanio
        CHECK (tamanio_bytes > 0)
);

CREATE INDEX idx_imagen_elemento
    ON imagen (tipo_elemento, elemento_id);
