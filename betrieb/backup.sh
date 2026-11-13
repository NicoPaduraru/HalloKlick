#!/bin/sh
# Nächtliches, verschlüsseltes Backup der Datenbank (Bauplan, Schritt 6.4).
#
# Aufruf im Ordner "betrieb":   sh backup.sh
# Einrichten als nächtliche Aufgabe (cron), zum Beispiel um 2:30 Uhr:
#   30 2 * * * cd /pfad/zu/digitalbegleiter/betrieb && sh backup.sh >> backup.log 2>&1
#
# Das Backup wird mit dem Passwort BACKUP_PASSWORT aus der Datei .env verschlüsselt (AES-256).
# Backups, die älter als 14 Tage sind, werden entfernt.
set -eu

cd "$(dirname "$0")"
. ./.env

if [ -z "${BACKUP_PASSWORT:-}" ]; then
  echo "FEHLER: BACKUP_PASSWORT ist in .env nicht gesetzt." >&2
  exit 1
fi

ZIEL=backups
mkdir -p "$ZIEL"
DATEI="$ZIEL/digitalbegleiter-$(date +%Y-%m-%d_%H%M).sql.gz.enc"

docker compose -f docker-compose.prod.yml exec -T db \
    pg_dump -U digitalbegleiter --no-owner digitalbegleiter \
  | gzip \
  | BACKUP_PASSWORT="$BACKUP_PASSWORT" openssl enc -aes-256-cbc -pbkdf2 -salt -pass env:BACKUP_PASSWORT -out "$DATEI"

# Ein leeres Backup ist kein Backup
if [ ! -s "$DATEI" ]; then
  echo "FEHLER: Das Backup $DATEI ist leer." >&2
  exit 1
fi

find "$ZIEL" -name 'digitalbegleiter-*.sql.gz.enc' -mtime +14 -delete
echo "$(date) Backup fertig: $DATEI"
