-- Permite recuperar un backend anterior sin quitar los códigos ni perder datos.
-- El backend nuevo envía una reserva explícita; este default cubre INSERT antiguos.
CREATE FUNCTION reservar_codigo_publico_compatible(prefijo TEXT) RETURNS TEXT AS $$
DECLARE
    candidato TEXT;
    intento INTEGER;
BEGIN
    IF prefijo IS NULL OR prefijo NOT IN ('PRY', 'EVT') THEN
        RAISE EXCEPTION 'Prefijo de código público no admitido' USING ERRCODE = '22023';
    END IF;
    FOR intento IN 1..20 LOOP
        candidato := prefijo || '-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));
        INSERT INTO public.codigos_publicos(codigo) VALUES (candidato) ON CONFLICT DO NOTHING;
        IF FOUND THEN RETURN candidato; END IF;
    END LOOP;
    RAISE EXCEPTION 'No se pudo reservar un código público' USING ERRCODE = '54000';
END;
$$ LANGUAGE plpgsql SET search_path = public, pg_temp;

ALTER TABLE proyectos ALTER COLUMN codigo_publico SET DEFAULT reservar_codigo_publico_compatible('PRY');
ALTER TABLE eventos ALTER COLUMN codigo_publico SET DEFAULT reservar_codigo_publico_compatible('EVT');
