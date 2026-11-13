-- BEISPIEL-INHALTE.
-- Diese Anleitungen sind Platzhalter, bis die Liste der häufigsten Probleme aus Phase 0 vorliegt.
-- Sie sind noch nicht mit echten Geräten und echten Senioren geprüft (geprueft_am ist leer).
-- Auf Android heißen Menüpunkte je nach Hersteller etwas anders.

CREATE FUNCTION pg_temp.neue_anleitung(p_titel text, p_kategorie text, p_plattform text, p_stichworte text,
                                       p_schritte text[]) RETURNS void AS $$
DECLARE
    neue_id uuid := gen_random_uuid();
BEGIN
    INSERT INTO anleitung (id, titel, kategorie, plattform, quelle, stichworte)
    VALUES (neue_id, p_titel, p_kategorie, p_plattform, 'SYSTEM', p_stichworte);
    INSERT INTO anleitung_schritt (anleitung_id, nummer, text)
    SELECT neue_id, s.nummer, s.text
    FROM unnest(p_schritte) WITH ORDINALITY AS s(text, nummer);
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.neue_anleitung('Ein Foto mit WhatsApp schicken', 'WhatsApp', 'ANDROID',
    'Bild senden verschicken Fotos schicken Galerie',
    ARRAY[
        'Öffnen Sie WhatsApp. Das Zeichen ist grün mit einem weißen Telefonhörer.',
        'Tippen Sie auf die Person, der Sie das Foto schicken möchten.',
        'Tippen Sie unten rechts auf die Büroklammer.',
        'Tippen Sie auf „Galerie“.',
        'Tippen Sie auf das Foto, das Sie schicken möchten.',
        'Tippen Sie unten rechts auf den grünen Pfeil. Das Foto ist jetzt unterwegs.'
    ]);

SELECT pg_temp.neue_anleitung('Ein Foto mit WhatsApp schicken', 'WhatsApp', 'IOS',
    'Bild senden verschicken Fotos schicken Mediathek',
    ARRAY[
        'Öffnen Sie WhatsApp. Das Zeichen ist grün mit einem weißen Telefonhörer.',
        'Tippen Sie auf die Person, der Sie das Foto schicken möchten.',
        'Tippen Sie unten links auf das Plus-Zeichen.',
        'Tippen Sie auf „Fotos“.',
        'Tippen Sie auf das Foto, das Sie schicken möchten.',
        'Tippen Sie unten rechts auf den Pfeil. Das Foto ist jetzt unterwegs.'
    ]);

SELECT pg_temp.neue_anleitung('Einen Videoanruf mit WhatsApp machen', 'WhatsApp', 'ALLE',
    'Video anrufen Videotelefonie sehen Kamera Anruf',
    ARRAY[
        'Öffnen Sie WhatsApp.',
        'Tippen Sie auf die Person, die Sie anrufen möchten.',
        'Tippen Sie oben rechts auf die kleine Kamera.',
        'Warten Sie, bis die Person abnimmt. Halten Sie das Handy so, dass Ihr Gesicht zu sehen ist.',
        'Zum Auflegen tippen Sie auf den roten Hörer.'
    ]);

SELECT pg_temp.neue_anleitung('Eine Sprachnachricht mit WhatsApp schicken', 'WhatsApp', 'ALLE',
    'sprechen aufnehmen Mikrofon Sprachmemo Audio',
    ARRAY[
        'Öffnen Sie WhatsApp und tippen Sie auf die Person.',
        'Unten rechts sehen Sie ein Mikrofon. Legen Sie den Finger darauf und lassen Sie ihn dort.',
        'Sprechen Sie jetzt Ihre Nachricht.',
        'Nehmen Sie den Finger weg. Die Nachricht wird sofort geschickt.'
    ]);

SELECT pg_temp.neue_anleitung('Die Schrift größer machen', 'Einstellungen', 'ANDROID',
    'Schriftgröße lesen klein groß Text vergrößern Anzeige',
    ARRAY[
        'Öffnen Sie die „Einstellungen“. Das Zeichen ist ein Zahnrad.',
        'Tippen Sie auf „Anzeige“. Bei manchen Handys heißt es „Display“.',
        'Tippen Sie auf „Schriftgröße“.',
        'Schieben Sie den Regler nach rechts, bis Sie die Schrift gut lesen können.'
    ]);

SELECT pg_temp.neue_anleitung('Die Schrift größer machen', 'Einstellungen', 'IOS',
    'Schriftgröße lesen klein groß Text vergrößern Anzeige',
    ARRAY[
        'Öffnen Sie die „Einstellungen“. Das Zeichen ist ein graues Zahnrad.',
        'Tippen Sie auf „Anzeige & Helligkeit“.',
        'Tippen Sie auf „Textgröße“.',
        'Schieben Sie den Regler nach rechts, bis Sie die Schrift gut lesen können.'
    ]);

SELECT pg_temp.neue_anleitung('Das Handy mit dem WLAN verbinden', 'Internet', 'ANDROID',
    'Internet WiFi Netzwerk verbinden Router Passwort',
    ARRAY[
        'Öffnen Sie die „Einstellungen“. Das Zeichen ist ein Zahnrad.',
        'Tippen Sie auf „Netzwerk & Internet“. Bei manchen Handys heißt es „Verbindungen“.',
        'Tippen Sie auf „WLAN“.',
        'Tippen Sie auf den Namen Ihres WLANs. Er steht meistens auf der Rückseite Ihres Routers.',
        'Tippen Sie das WLAN-Passwort ein. Auch das steht meistens auf dem Router.',
        'Tippen Sie auf „Verbinden“.'
    ]);

SELECT pg_temp.neue_anleitung('Das Handy mit dem WLAN verbinden', 'Internet', 'IOS',
    'Internet WiFi Netzwerk verbinden Router Passwort',
    ARRAY[
        'Öffnen Sie die „Einstellungen“. Das Zeichen ist ein graues Zahnrad.',
        'Tippen Sie auf „WLAN“.',
        'Tippen Sie auf den Namen Ihres WLANs. Er steht meistens auf der Rückseite Ihres Routers.',
        'Tippen Sie das WLAN-Passwort ein. Auch das steht meistens auf dem Router.',
        'Tippen Sie oben rechts auf „Verbinden“.'
    ]);

SELECT pg_temp.neue_anleitung('Ein Bildschirmfoto machen', 'Fotos', 'ANDROID',
    'Screenshot Bildschirm fotografieren aufnehmen',
    ARRAY[
        'Öffnen Sie das, was Sie fotografieren möchten, zum Beispiel eine Nachricht.',
        'Drücken Sie gleichzeitig kurz die Ein-/Aus-Taste und die Leiser-Taste.',
        'Der Bildschirm blinkt kurz. Das Bildschirmfoto liegt jetzt bei Ihren Fotos.'
    ]);

SELECT pg_temp.neue_anleitung('Ein Bildschirmfoto machen', 'Fotos', 'IOS',
    'Screenshot Bildschirm fotografieren aufnehmen',
    ARRAY[
        'Öffnen Sie das, was Sie fotografieren möchten, zum Beispiel eine Nachricht.',
        'Drücken Sie gleichzeitig kurz die Seitentaste und die Lauter-Taste. Hat Ihr iPhone unten einen runden Knopf? Dann drücken Sie die Seitentaste und den runden Knopf.',
        'Der Bildschirm blinkt kurz. Das Bildschirmfoto liegt jetzt bei Ihren Fotos.'
    ]);

SELECT pg_temp.neue_anleitung('Eine Telefonnummer speichern', 'Telefon', 'ANDROID',
    'Kontakt anlegen neu Nummer eintragen Adressbuch',
    ARRAY[
        'Öffnen Sie „Kontakte“.',
        'Tippen Sie auf das Plus-Zeichen.',
        'Tippen Sie den Namen ein.',
        'Tippen Sie die Telefonnummer ein.',
        'Tippen Sie auf „Speichern“.'
    ]);

SELECT pg_temp.neue_anleitung('Eine Telefonnummer speichern', 'Telefon', 'IOS',
    'Kontakt anlegen neu Nummer eintragen Adressbuch',
    ARRAY[
        'Öffnen Sie „Kontakte“.',
        'Tippen Sie oben rechts auf das Plus-Zeichen.',
        'Tippen Sie den Namen ein.',
        'Tippen Sie auf „Telefon hinzufügen“ und tippen Sie die Nummer ein.',
        'Tippen Sie oben rechts auf „Fertig“.'
    ]);

SELECT pg_temp.neue_anleitung('Das Handy lauter stellen', 'Einstellungen', 'ALLE',
    'Lautstärke Ton leise laut Klingelton hören',
    ARRAY[
        'An der Seite Ihres Handys sind zwei Tasten für die Lautstärke.',
        'Drücken Sie mehrmals auf die obere Taste. Auf dem Bildschirm sehen Sie einen Balken, der länger wird.',
        'Hören Sie den Klingelton trotzdem nicht? Dann fragen Sie Ihre Familie. Vielleicht ist das Handy auf „Lautlos“ gestellt.'
    ]);
