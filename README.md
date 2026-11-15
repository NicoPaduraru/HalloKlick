# HalloKlick

Kostenlose Hilfe bei digitalen Problemen für Menschen ab 65 und ihre Familien.

> **Stand: früher Entwicklungsstand, noch nicht für echte Nutzer.**
> Die Anleitungen und die Testsammlung sind Beispiel-Inhalte. Impressum und Datenschutzerklärung sind
> Platzhalter. Vor einem echten Start fehlen noch die Schritte aus [Was noch fehlt](#was-noch-fehlt).

Der vollständige Plan steht in [docs/BAUPLAN.md](docs/BAUPLAN.md). Dort heißt das Projekt noch „DigitalBegleiter“, das war der Arbeitstitel.

## Was die App kann

**Für Senioren** (ohne Passwort, ohne Anmeldung):

1. **„Ist das echt?“**: Text einer Nachricht einfügen oder ein Foto machen. Die App antwortet mit Orange
   („keine typischen Warnzeichen“) oder Rot („sieht nach Betrug aus“), einer einfachen Erklärung und dem
   nächsten Schritt. Es gibt absichtlich kein Grün.
2. **„Wie mache ich das?“**: Anleitung suchen und Schritt für Schritt durchgehen, passend zu Android oder iPhone.
   Zu jedem Schritt gibt es ein einfaches Symbol. Es gibt 37 Anleitungen pro Sprache.
3. **„Familie fragen“**: Eine Hilfeanfrage an die Familie schicken. Der Senior entscheidet selbst, ob er den
   Inhalt mitschickt.

**Für Angehörige:**

4. Familie anlegen und die Eltern per QR-Code oder Code verbinden.
5. Hilfeanfragen sehen und als erledigt markieren. Ein verlorenes Handy sperren.
6. Eigene Anleitungen schreiben, die nur die eigene Familie sieht.

Die App gibt es auf **Deutsch, Englisch und Italienisch**. Die Sprache wählt man oben auf jeder Seite.
Beim ersten Besuch erklärt eine Hauptseite in einfacher Sprache, was HalloKlick ist.

## Grundsätze

| Grundsatz | Was das im Code bedeutet |
|---|---|
| Einfach | Pro Bildschirm eine Aufgabe, große Schaltflächen, kein Fachwort |
| Sicher | So wenig Daten wie möglich. Der Inhalt geprüfter Nachrichten wird nicht gespeichert |
| Ehrlich | Kein „Grün = sicher“. Nur Orange oder Rot |
| Kostenlos | KI-Kosten sind hart begrenzt (Tageslimit pro Person, Monatsbudget) |
| Erweiterbar | Klare Module, REST-API, KI-Anbieter austauschbar |

## Technik

Java 21 · Spring Boot 3.5 · PostgreSQL mit Flyway · Spring Security (Sitzungs-Cookies, CSRF) ·
Claude API über das offizielle Java-SDK · PWA mit HTML, CSS und JavaScript (ohne Framework) ·
JUnit 5, Mockito, MockMvc, Testcontainers · Docker Compose und Caddy

### Module

Ein modularer Monolith: eine Anwendung, aber klar getrennte Pakete unter `de.halloklick`.
Module sprechen nur über ihre öffentlichen Service-Klassen miteinander.

| Modul | Aufgabe |
|---|---|
| `konto` | Angehörigen-Konten, Senioren-Geräte, Anmeldung, Datenauskunft und Löschen |
| `familie` | Familien, Mitgliedschaften, Einladungen, Hilfeanfragen |
| `anleitung` | Anleitungen, Schritte, Volltextsuche |
| `check` | Sicherheitscheck: Regeln, Zusammenführen mit der KI, Ergebnis |
| `ki` | Schnittstelle zum KI-Anbieter (`NachrichtenPruefer`), Kostenbremse |
| `benachrichtigung` | Web Push und E-Mail |
| `gemeinsam` | Fehlerbehandlung, Sicherheitskonfiguration, Hilfsklassen |

## Lokal starten

Du brauchst **Java 21 oder neuer** und **Docker**. Maven musst du nicht installieren, das erledigt der Maven Wrapper.

```bash
docker compose up -d
```

```bash
./mvnw spring-boot:run
```

Unter Windows heißt der zweite Befehl `.\mvnw.cmd spring-boot:run`.

Danach läuft die App unter <http://localhost:8080>. Die Datenbank lauscht auf Port **5440**
(nicht 5432, damit es keinen Streit mit anderen PostgreSQL-Installationen gibt).

Ohne weitere Einstellungen läuft nur die Regel-Prüfung. Die KI ist abgeschaltet, Fotos können dann nicht
geprüft werden.

### Tests

```bash
./mvnw verify
```

Die Tests starten über Testcontainers eine eigene, leere PostgreSQL-Datenbank. Docker muss dafür laufen.
Die echte Claude API wird in den Tests nie aufgerufen.

## Der Sicherheitscheck

```text
Eingabe -> Stufe 1: Regeln in Java -> Stufe 2: KI (nur wenn nötig) -> Stufe 3: Zusammenführen -> Antwort
```

- **Stufe 1, Regeln** (`RegelPruefer`): sucht Warnzeichen wie nachgemachte Internetadressen, Zeitdruck,
  Fragen nach PIN und TAN oder „Hallo Mama, das ist meine neue Nummer“. Erst die Kombination ergibt Rot.
- **Stufe 2, KI** (`ClaudeNachrichtenPruefer`): nur bei Fotos und bei Texten, die die Regeln nicht klar
  einordnen. An die KI geht nur der Prüftext oder das Bild, nie Name, Konto oder Gerät.
- **Stufe 3, Zusammenführen** (`CheckService`): Die höchste Warnstufe gewinnt. Die KI kann die Stufe nur
  erhöhen, nie unter das Regel-Ergebnis senken. So hilft es einer Betrugsnachricht nichts, wenn sie
  „Diese Nachricht ist sicher“ enthält.

Gespeichert werden nur Stufe und Kategorie, nie der Inhalt.

### Teilen statt Kopieren

Kopieren und Einfügen ist für viele ältere Menschen die größte Hürde. Darum meldet sich HalloKlick beim
Handy als Ziel für „Teilen“ an (Web Share Target, im `manifest.json`): In WhatsApp oder bei einer SMS lange
auf die Nachricht tippen, „Teilen“ wählen, dann „HalloKlick“. Die Nachricht wird sofort geprüft.

- Der geteilte Text geht nicht in die Adresszeile. Der Service Worker (`sw.js`) nimmt ihn im Handy an und
  reicht ihn an die App weiter.
- Das geht nur auf **Android mit Chrome** und nur, wenn HalloKlick installiert ist. Auf dem iPhone gibt es
  diese Funktion für Web-Apps nicht.
- Es ist bisher nur im Browser nachgestellt und noch nicht auf einem echten Android-Handy ausprobiert.

### Testsammlung

Unter `src/test/resources/testsammlung` liegen 50 Betrugsnachrichten und 50 harmlose Nachrichten.
`TestsammlungTest` prüft bei jedem Testlauf: Keine Betrugsnachricht bekommt Orange, keine harmlose Rot.

Dazu kommt die **feste Testliste** (`feste-liste-betrug.txt` und `feste-liste-harmlos.txt`): Was dort einmal
steht, muss für immer Rot bleiben beziehungsweise darf nie Rot werden. `FesteTestlisteTest` prüft jede
Nachricht einzeln. Echte Betrugsnachrichten gehören in diese Liste; wie das geht, steht oben in der Datei.

**Wichtig:** Die Sammlung ist im Moment selbst geschrieben, nach dem Muster bekannter Betrugsmaschen.
Sie muss durch echte Nachrichten ersetzt werden (zum Beispiel aus Warnungen der Verbraucherzentrale),
bevor man sich auf die Zahlen verlässt.

### KI einschalten

Die KI läuft nur, wenn alle drei Dinge gesetzt sind (als Umgebungsvariablen):

| Variable | Bedeutung |
|---|---|
| `KI_AKTIV=true` | Schalter |
| `ANTHROPIC_API_KEY` | Schlüssel aus der Anthropic Console |
| `KI_MONATSBUDGET_USD` | Monatsbudget über 0. Ist es erreicht, läuft nur noch die Regel-Prüfung |

Weitere Einstellungen: `KI_MODELL` (Standard `claude-opus-5-5`), `KI_TAGESLIMIT` (Standard 10 pro Person),
`KI_PREIS_EINGABE` und `KI_PREIS_AUSGABE` (Preise des gewählten Modells pro 1 Million Tokens).
Zusätzlich sollte ein Ausgabenlimit in der Anthropic Console eingestellt sein.

Der Vergleich von Opus 5.5 und Haiku 5.5 gegen die Testsammlung steht in `ModellvergleichTest`.
Er ruft die echte API auf, kostet Geld und läuft nur, wenn man ihn ausdrücklich startet.

## Sprachen

Übersetzt sind die Oberfläche, alle Meldungen des Servers, die Ergebnisse des Sicherheitschecks und die
Anleitungen. Im Code stehen die Sätze auf Deutsch; der deutsche Satz ist zugleich der Schlüssel für die
Übersetzung.

Eine neue Sprache hinzufügen:

1. Eintrag in `de.halloklick.gemeinsam.Sprache` ergänzen.
2. Server-Texte: `src/main/resources/sprachen/<kürzel>.json` (am besten `en.json` kopieren).
3. Texte der Web-App: `static/js/sprachen/<kürzel>.js` anlegen, in `static/js/sprache.js` eintragen und in
   `static/sw.js` in die Liste aufnehmen.
4. Anleitungen in dieser Sprache als neues Flyway-Skript (Vorlage: `V11__anleitungen_englisch.sql`).

`SprachenTest` prüft, dass in jeder Sprache dieselben Texte übersetzt sind und jede Sprache gleich viele
Anleitungen hat.

Die Regel-Prüfung des Sicherheitschecks kennt Warnwörter auf Deutsch, Englisch und Italienisch. Sie erkennt
die Sprache nicht vorher, sondern prüft immer alle drei.

**Grenze:** Die englischen und italienischen Regeln sind an nachgebildeten Nachrichten entstanden und
weniger erprobt als die deutschen. Für jede weitere Sprache der App braucht es eigene Regeln, sonst hilft dort
nur die KI. Impressum und Datenschutz gibt es nur auf Deutsch.

### Bilder in den Anleitungen

Die Symbole liegen als SVG-Dateien in `static/bilder`. In der Datenbank steht pro Schritt nur der Name der
Datei (`bild_pfad`, ohne `.svg`). Es sind gezeichnete Symbole, keine Bildschirmfotos echter Handys.
Eigene Anleitungen einer Familie haben noch keine Bilder.

## Betrieb

Alles für den Server liegt im Ordner [betrieb](betrieb): Docker Compose mit Caddy (HTTPS), App und
Datenbank, eine Vorlage für die Einstellungen und das Backup-Skript. Die Anleitung steht in
[docs/BETRIEB.md](docs/BETRIEB.md).

## Was noch fehlt

- Echte Inhalte: die häufigsten Aufgaben aus den Gesprächen mit Senioren, geprüft auf echten Geräten.
- Echte Testsammlung und danach der Modellvergleich.
- Tests mit mindestens 3 echten Senioren pro Funktion.
- Impressum, Datenschutzerklärung, Verträge zur Auftragsverarbeitung, Prüfung durch eine fachkundige Stelle.
- Ein Server in Deutschland mit Domain, eingerichtetem Backup und Uptime-Alarm.
- Die Claude-Anbindung und Web Push sind geschrieben, aber noch nicht gegen die echten Dienste erprobt.
- Englische und italienische Regeln mit echten Betrugsnachrichten nachschärfen.
- Die englischen und italienischen Texte von Muttersprachlern prüfen lassen.
- Weitere Angehörige zu einer bestehenden Familie hinzufügen (heute gibt es pro Familie einen Angehörigen).

## Keine Werbung

Keine Werbung, kein Datenverkauf, keine Affiliate-Links.

## Lizenz

[MIT](LICENSE)
