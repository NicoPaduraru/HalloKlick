# Betrieb: HalloKlick auf einem Server

Diese Anleitung gehört zu Bauphase B6. Sie beschreibt den Aufbau aus dem Bauplan (Abschnitt 12):

```text
Internet --443--> Caddy (HTTPS automatisch) --> Spring Boot --> PostgreSQL --> nächtliches, verschlüsseltes Backup
```

> Diese Anleitung ist noch nicht auf einem echten Server erprobt. Probiere jeden Schritt zuerst in der
> Umgebung `test` aus, bevor echte Nutzer dazukommen.

## 1. Voraussetzungen

- Ein Server mit Standort Deutschland, auf dem Docker läuft.
- Eine Domain, die auf den Server zeigt. Die Ports 80 und 443 müssen offen sein.

## 2. Einrichten

Auf dem Server das Repository holen und im Ordner `betrieb` die Einstellungen anlegen:

```bash
cp .env.example .env
```

Dann `.env` ausfüllen. Mindestens `DOMAIN`, `DB_PASSWORD` und `BACKUP_PASSWORT`. Passwörter und
API-Schlüssel stehen nur in dieser Datei, nie im Code. Die Datei `.env` wird von Git ignoriert.

Starten:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Caddy holt das HTTPS-Zertifikat von selbst. Prüfen, ob alles läuft:

```bash
curl https://DEINE-DOMAIN/actuator/health
```

Die Antwort muss `{"status":"UP"}` enthalten.

## 3. Zwei Umgebungen

`test` (für dich) und `prod` (für Nutzer) sind zwei getrennte Ordner oder Server mit je eigener `.env`,
eigener Domain und eigener Datenbank. Neue Versionen kommen immer zuerst nach `test`.

## 4. Backup

Das Skript `backup.sh` schreibt ein verschlüsseltes Backup in den Ordner `betrieb/backups` und entfernt
Backups, die älter als 14 Tage sind. Als nächtliche Aufgabe einrichten (`crontab -e`):

```text
30 2 * * * cd /pfad/zu/halloklick/betrieb && sh backup.sh >> backup.log 2>&1
```

Die Backups sollten zusätzlich auf einen zweiten Speicherort kopiert werden. Ein Backup auf demselben
Server hilft nicht, wenn der Server ausfällt.

### Wiederherstellung einmal ausprobieren

Ein Backup ist erst etwas wert, wenn die Wiederherstellung einmal geklappt hat. In der Umgebung `test`:

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:BACKUP_PASSWORT -in backups/DATEI.sql.gz.enc | gunzip | docker compose -f docker-compose.prod.yml exec -T db psql -U digitalbegleiter digitalbegleiter
```

Vorher muss die Datenbank leer sein. Danach in der App prüfen, ob die Daten da sind.

## 5. Überwachung

- **Gesundheits-Check:** `https://DEINE-DOMAIN/actuator/health`. Ein Uptime-Dienst ruft die Adresse
  regelmäßig auf und schlägt Alarm, wenn sie nicht antwortet. Alle anderen `/actuator`-Seiten sind gesperrt.
- **KI-Kosten:** interne Übersicht mit Aufrufen und geschätzten Kosten pro Tag. Dafür in `.env` einen
  `INTERN_TOKEN` mit mindestens 20 Zeichen setzen:

```bash
curl -H "X-Intern-Token: DEIN-TOKEN" https://DEINE-DOMAIN/api/intern/ki-nutzung
```

## 6. Web Push einrichten

Für Benachrichtigungen braucht der Server ein Schlüsselpaar (VAPID). Einmalig erzeugen:

```bash
docker run --rm node:22-alpine npx -y web-push generate-vapid-keys
```

Den öffentlichen Schlüssel als `PUSH_OEFFENTLICH`, den privaten als `PUSH_PRIVAT` in `.env` eintragen.
`PUSH_BETREFF` ist eine Kontaktadresse in der Form `mailto:...`. Ohne diese Schlüssel gibt es keine
Push-Nachrichten. Ist E-Mail eingerichtet (`MAIL_...`), geht die Benachrichtigung dann per E-Mail.

## 7. Was die Technik für den Datenschutz tut

| Maßnahme | Wo |
|---|---|
| Inhalt geprüfter Nachrichten wird nicht gespeichert | Tabelle `sicherheitscheck` hat keine Spalte dafür |
| Hilfeanfragen werden nach 30 Tagen gelöscht | `HilfeanfrageService`, jede Nacht um 3:00 Uhr |
| Inaktive Geräte werden nach 12 Monaten gelöscht | `GeraeteService`, jede Nacht um 3:30 Uhr |
| Datenauskunft und Konto löschen per Knopfdruck | `GET /api/ich/daten`, `DELETE /api/ich` |
| Passwörter und Schlüssel nur als Hash | BCrypt für Passwörter, SHA-256 für Geräte-Schlüssel und Einladungscodes |
| Logs ohne Nachrichteninhalte und E-Mail-Adressen | Fehler werden nur mit ihrem Typ geloggt, Caddy schreibt kein Zugriffsprotokoll |
| Begrenzung der Anfragen pro Minute | `AnfrageBegrenzung` |

## 8. Bekannte Grenzen

- Die Anmeldung der Angehörigen und die Zähler der Anfrage-Begrenzung liegen im Arbeitsspeicher.
  Nach einem Neustart müssen sich Angehörige neu anmelden. Die Geräte der Senioren bleiben angemeldet.
- Es läuft genau eine Instanz der App. Für mehrere Instanzen müssten Sitzungen und Zähler in eine
  gemeinsame Ablage.

## 9. Rechtliches (vor dem echten Start)

Das kann die Software nicht erledigen:

- Datenschutzerklärung und Impressum (die Seiten in der App sind Platzhalter).
- Verträge zur Auftragsverarbeitung mit dem Hosting-Anbieter und mit Anthropic. Dabei prüfen, wie lange
  Anthropic Daten speichert und welche Bedingungen gelten.
- Verzeichnis der Verarbeitungstätigkeiten.
- Prüfung durch eine fachkundige Stelle, zum Beispiel die Gründungsberatung der Hochschule.
