#!/usr/bin/env bash
# Despliegue V4 -> V6 con respaldo privado; conserva configuración y datos previos.
# Uso con sudo: bash deploy-public-codes.sh /ruta/candidato.jar SHA256 /ruta/backup-helper.sh
set -euo pipefail
umask 077
[[ "$EUID" -eq 0 && "$#" -eq 3 ]] || { echo 'Se requieren sudo, candidato, SHA256 y helper de respaldo.' >&2; exit 1; }
candidate=$(realpath -- "$1")
expected="$2"
helper=$(realpath -- "$3")
[[ -s "$candidate" && -f "$helper" && "$expected" =~ ^[a-fA-F0-9]{64}$ ]] || exit 1
actual=$(sha256sum "$candidate" | cut -d ' ' -f 1)
[[ "$actual" == "${expected,,}" ]] || { echo 'El candidato no coincide con el SHA256 esperado.' >&2; exit 1; }
version() { runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At -c 'SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1'; }
[[ "$(version)" == 4 ]] || { echo 'Este despliegue exige esquema inicial V4.' >&2; exit 1; }
[[ "$(grep '^ATTENDANCE_GEO_MODE=' /etc/syncpass/backend.env)" == 'ATTENDANCE_GEO_MODE=ENFORCE' ]] || {
  echo 'Se esperaba ENFORCE explícito. Revisar configuración antes de desplegar.' >&2; exit 1;
}
systemctl is-active --quiet nginx
systemctl is-active --quiet syncpass
config_hash=$(sha256sum /etc/syncpass/backend.env | cut -d ' ' -f 1)
backup=''
paused=false
swapped=false
healthy() {
  for attempt in $(seq 1 45); do
    if curl -fsS --connect-timeout 2 --max-time 3 http://127.0.0.1:8080/actuator/health > /dev/null 2>&1; then return 0; fi
    sleep 2
  done
  return 1
}
recover() {
  status=$?
  trap - ERR
  set +e
  if [[ "$swapped" == true && -f "$backup/backend.jar" ]]; then
    schema=$(version)
    if [[ "$schema" == 4 || "$schema" == 6 ]]; then
      systemctl stop syncpass
      install -o root -g root -m 0644 "$backup/backend.jar" /opt/syncpass/backend.jar.recovery
      mv /opt/syncpass/backend.jar.recovery /opt/syncpass/backend.jar
      systemctl start syncpass
      if healthy; then echo 'Se recuperó el JAR anterior; no se restauró ni sobrescribió la base.' >&2; fi
    else
      echo 'Estado de esquema intermedio: requiere revisión; no se restaura la base automáticamente.' >&2
    fi
  elif [[ "$paused" == true ]]; then
    systemctl start syncpass
  fi
  if [[ "$paused" == true ]]; then systemctl start nginx; fi
  printf 'Despliegue interrumpido; revisar servicio y respaldo privado: %s\n' "$backup" >&2
  exit "$status"
}
trap recover ERR
snapshot() {
  runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At <<'SQL'
SELECT format('SELECT %L, count(*), md5(COALESCE(string_agg(row_data, '''' ORDER BY row_data), '''')) FROM (SELECT (to_jsonb(t) - ''codigo_publico'')::text AS row_data FROM %I t) rows;', tablename, tablename)
FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('flyway_schema_history','codigos_publicos') ORDER BY tablename
\gexec
SQL
}
# Sin recepción externa durante la comparación: ningún cliente escribe entre snapshots.
systemctl stop nginx
paused=true
systemctl stop syncpass
output=$(bash "$helper")
printf '%s\n' "$output"
backup=$(printf '%s\n' "$output" | sed -n 's/^Respaldo completado en directorio privado: //p')
[[ "$backup" == /var/backups/syncpass/pre-migrations-* && -f "$backup/database.dump" && -f "$backup/backend.jar" ]]
snapshot > "$backup/business-before.txt"
install -o root -g root -m 0644 "$candidate" /opt/syncpass/backend.jar.next
mv /opt/syncpass/backend.jar.next /opt/syncpass/backend.jar
swapped=true
systemctl start syncpass
healthy
[[ "$(version)" == 6 ]]
snapshot > "$backup/business-after.txt"
diff -u "$backup/business-before.txt" "$backup/business-after.txt"
[[ "$(sha256sum /etc/syncpass/backend.env | cut -d ' ' -f 1)" == "$config_hash" ]]
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At <<'SQL'
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM proyectos WHERE codigo_publico !~ '^PRY-[0-9A-F]{12}$')
     OR EXISTS (SELECT 1 FROM eventos WHERE codigo_publico !~ '^EVT-[0-9A-F]{12}$') THEN
    RAISE EXCEPTION 'Se encontraron códigos con formato inválido';
  END IF;
  IF (SELECT count(*) FROM codigos_publicos) <> ((SELECT count(*) FROM proyectos)+(SELECT count(*) FROM eventos)) THEN
    RAISE EXCEPTION 'Las reservas iniciales no coinciden con los recursos migrados';
  END IF;
END $$;
SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT 'proyectos',count(*),count(DISTINCT codigo_publico) FROM proyectos;
SELECT 'eventos',count(*),count(DISTINCT codigo_publico) FROM eventos;
SQL
curl -fsS http://127.0.0.1:8080/actuator/health > "$backup/health-after.json"
systemctl start nginx
paused=false
trap - ERR
sha256sum /opt/syncpass/backend.jar
printf 'Despliegue V6 saludable; datos previos y configuración conservados. Respaldo: %s\n' "$backup"
