-- Campos opcionales: conserva las asistencias históricas y no reescribe sus datos.
ALTER TABLE asistencias ADD COLUMN ubicacion_en TIMESTAMP WITH TIME ZONE;
ALTER TABLE asistencias ADD COLUMN validacion_geografica VARCHAR(32);
ALTER TABLE asistencias ADD COLUMN motivo_geografico VARCHAR(64);
