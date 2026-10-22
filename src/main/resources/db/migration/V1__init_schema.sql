CREATE TABLE edificio (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo      VARCHAR(50)  NOT NULL UNIQUE,
    nombre      VARCHAR(100) NOT NULL,
    descripcion TEXT,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE zona_segura (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(100) NOT NULL,
    ubicacion   VARCHAR(255) NOT NULL,
    descripcion TEXT         NOT NULL,
    capacidad   INTEGER      NOT NULL CHECK (capacidad > 0),
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO'
);