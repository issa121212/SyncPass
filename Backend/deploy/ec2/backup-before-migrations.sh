#!/usr/bin/env bash
# Ejecutar con sudo en EC2. No modifica el esquema ni reinicia servicios.
set -euo pipefail
umask 077
if [[ "$EUID" -ne 0 ]]; then
  echo 'Ejecuta este script con sudo.' >&2
  exit 1
fi
for file in /etc/syncpass/backend.env /opt/syncpass/backend.jar; do
  [[ -f "$file" ]] || { echo "No existe el archivo esperado: $file" >&2; exit 1; }
done
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At -c 'SELECT current_database()' >/dev/null
backup="/var/backups/syncpass/pre-migrations-$(date -u +%Y%m%dT%H%M%S)-$$"
install -d -m 0700 -o root -g root "$backup"
trap 'echo "Respaldo incompleto; revisar el directorio privado: $backup" >&2' ERR
runuser -u postgres -- pg_dump --no-password --format=custom --dbname=syncpass > "$backup/database.dump.partial"
pg_restore --list "$backup/database.dump.partial" > "$backup/database.manifest.txt"
mv -- "$backup/database.dump.partial" "$backup/database.dump"
cp -- /etc/syncpass/backend.env "$backup/backend.env"
cp -- /opt/syncpass/backend.jar "$backup/backend.jar"
chmod 0600 "$backup/backend.env" "$backup/backend.jar"
systemctl cat syncpass > "$backup/syncpass.service.txt"
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 -d syncpass -At -c 'SELECT version()' > "$backup/postgresql-version.txt"
sha256sum "$backup/database.dump" "$backup/backend.jar" "$backup/backend.env" > "$backup/SHA256SUMS"
printf 'Respaldo completado en directorio privado: %s\n' "$backup"
printf 'No se aplicaron migraciones ni se reinició el backend. Validar restauración sobre una copia antes de desplegar.\n'
