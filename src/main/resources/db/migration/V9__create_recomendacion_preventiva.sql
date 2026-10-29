CREATE TABLE recomendacion_preventiva (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ambito VARCHAR(20) NOT NULL,
    edificio_id UUID REFERENCES edificio(id),
    tipo_espacio VARCHAR(20),
    titulo VARCHAR(150) NOT NULL,
    recomendacion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_recomendacion_ambito
        CHECK (ambito IN ('EDIFICIO', 'TIPO_ESPACIO')),

    CONSTRAINT ck_recomendacion_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO')),

    CONSTRAINT ck_recomendacion_ambito_datos
        CHECK (
            (ambito = 'EDIFICIO'
                AND edificio_id IS NOT NULL
                AND tipo_espacio IS NULL)
            OR
            (ambito = 'TIPO_ESPACIO'
                AND edificio_id IS NULL
                AND tipo_espacio IS NOT NULL)
        ),

    CONSTRAINT ck_recomendacion_tipo_espacio
        CHECK (
            tipo_espacio IS NULL
            OR tipo_espacio IN (
                'SALON', 'LABORATORIO', 'AUDITORIO',
                'OFICINA', 'ESPACIO_COMUN'
            )
        )
);

CREATE INDEX idx_recomendacion_edificio
    ON recomendacion_preventiva(edificio_id);

CREATE INDEX idx_recomendacion_tipo_espacio
    ON recomendacion_preventiva(tipo_espacio);
