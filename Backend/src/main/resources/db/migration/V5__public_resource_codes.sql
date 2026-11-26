-- No cambia identificadores, relaciones, QR ni operaciones idempotentes existentes.
CREATE TABLE codigos_publicos (
    codigo VARCHAR(16) PRIMARY KEY,
    CONSTRAINT ck_codigo_publico_formato CHECK (codigo ~ '^(PRY|EVT)-[0-9A-F]{12}$')
);

ALTER TABLE proyectos ADD COLUMN codigo_publico VARCHAR(16);
ALTER TABLE eventos ADD COLUMN codigo_publico VARCHAR(16);

-- Función transitoria de migración: reserva con unicidad incluso ante una colisión.
CREATE FUNCTION reservar_codigo_migracion(prefijo TEXT) RETURNS TEXT AS $$
DECLARE candidato TEXT;
BEGIN
    LOOP
        candidato := prefijo || '-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));
        INSERT INTO codigos_publicos(codigo) VALUES (candidato) ON CONFLICT DO NOTHING;
        IF FOUND THEN RETURN candidato; END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

UPDATE proyectos SET codigo_publico = reservar_codigo_migracion('PRY');
UPDATE eventos SET codigo_publico = reservar_codigo_migracion('EVT');
DROP FUNCTION reservar_codigo_migracion(TEXT);

ALTER TABLE proyectos ALTER COLUMN codigo_publico SET NOT NULL;
ALTER TABLE eventos ALTER COLUMN codigo_publico SET NOT NULL;
ALTER TABLE proyectos ADD CONSTRAINT uk_proyecto_codigo_publico UNIQUE (codigo_publico);
ALTER TABLE eventos ADD CONSTRAINT uk_evento_codigo_publico UNIQUE (codigo_publico);
ALTER TABLE proyectos ADD CONSTRAINT fk_proyecto_codigo_publico FOREIGN KEY (codigo_publico) REFERENCES codigos_publicos(codigo);
ALTER TABLE eventos ADD CONSTRAINT fk_evento_codigo_publico FOREIGN KEY (codigo_publico) REFERENCES codigos_publicos(codigo);
