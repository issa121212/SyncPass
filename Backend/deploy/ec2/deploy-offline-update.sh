#!/usr/bin/env bash
# Despliegue explícito de V3/V4; conserva respaldo privado y compara datos previos.
set -euo pipefail
umask 077
[[ "$EUID" -eq 0 ]] || exit 1
candidate=/home/ec2-user/syncpass-candidate-20261126.jar
[[ -s "$candidate" ]] || exit 1
snapshot() {
  runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At <<'SQL'
SELECT format('SELECT %L, count(*), md5(COALESCE(string_agg(row_data, '''' ORDER BY row_data), '''')) FROM (SELECT (to_jsonb(t) - ''ubicacion_en'' - ''validacion_geografica'' - ''motivo_geografico'')::text AS row_data FROM %I t) rows;', tablename, tablename)
FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('flyway_schema_history','creaciones_operaciones') ORDER BY tablename
\gexec
SQL
}
systemctl stop syncpass
trap 'echo "Despliegue interrumpido. Revisar servicio y respaldo privado antes de continuar." >&2' ERR
bash /home/ec2-user/syncpass-backup-20261126.sh
backup=$(find /var/backups/syncpass -maxdepth 1 -type d -name 'pre-migrations-*' | sort | tail -1)
[[ -f "$backup/database.dump" && -f "$backup/backend.jar" ]] || exit 1
snapshot > "$backup/business-before.txt"
if grep -q '^ATTENDANCE_GEO_MODE=' /etc/syncpass/backend.env; then
  sed -i 's/^ATTENDANCE_GEO_MODE=.*/ATTENDANCE_GEO_MODE=AUDIT/' /etc/syncpass/backend.env
else
  printf '\nATTENDANCE_GEO_MODE=AUDIT\n' >> /etc/syncpass/backend.env
fi
install -o root -g root -m 0644 "$candidate" /opt/syncpass/backend.jar.next
mv /opt/syncpass/backend.jar.next /opt/syncpass/backend.jar
systemctl start syncpass
healthy=false
for attempt in $(seq 1 45); do
  if curl -fsS http://127.0.0.1:8080/actuator/health > "$backup/health-after.json"; then healthy=true; break; fi
  sleep 2
done
if [[ "$healthy" != true ]]; then
  echo "El backend no está saludable; respaldo conservado en $backup" >&2
  exit 1
fi
snapshot > "$backup/business-after.txt"
diff -u "$backup/business-before.txt" "$backup/business-after.txt"
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At -c 'SELECT version, success FROM flyway_schema_history ORDER BY installed_rank'
sha256sum /opt/syncpass/backend.jar
printf 'Despliegue saludable; datos anteriores sin cambios. Respaldo: %s\n' "$backup"
