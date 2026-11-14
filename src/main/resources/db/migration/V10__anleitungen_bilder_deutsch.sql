-- BEISPIEL-INHALTE, noch nicht an echten Geräten geprüft (geprueft_am bleibt leer).
-- 1. Die vorhandenen deutschen Anleitungen bekommen zu jedem Schritt ein Symbol.
-- 2. Es kommen 22 neue deutsche Anleitungen dazu.
--
-- Ein Symbol ist der Name einer Datei aus static/bilder (ohne ".svg").

-- Setzt die Symbole einer vorhandenen Anleitung, in der Reihenfolge der Schritte.
CREATE OR REPLACE FUNCTION pg_temp.symbole(p_titel text, p_plattform text, p_symbole text[]) RETURNS void AS $$
BEGIN
    UPDATE anleitung_schritt s
    SET bild_pfad = p_symbole[s.nummer]
    FROM anleitung a
    WHERE a.id = s.anleitung_id AND a.quelle = 'SYSTEM' AND a.sprache = 'de'
      AND a.titel = p_titel AND a.plattform = p_plattform;
END;
$$ LANGUAGE plpgsql;

-- Legt eine Anleitung an. Jeder Schritt hat die Form 'symbol|Text'.
CREATE OR REPLACE FUNCTION pg_temp.neu(p_titel text, p_kategorie text, p_plattform text, p_stichworte text,
                            p_schritte text[]) RETURNS void AS $$
DECLARE
    neue_id uuid := gen_random_uuid();
BEGIN
    INSERT INTO anleitung (id, titel, kategorie, plattform, quelle, stichworte, sprache)
    VALUES (neue_id, p_titel, p_kategorie, p_plattform, 'SYSTEM', p_stichworte, 'de');
    INSERT INTO anleitung_schritt (anleitung_id, nummer, text, bild_pfad)
    SELECT neue_id, s.nummer, substring(s.zeile from position('|' in s.zeile) + 1), split_part(s.zeile, '|', 1)
    FROM unnest(p_schritte) WITH ORDINALITY AS s(zeile, nummer);
END;
$$ LANGUAGE plpgsql;

-- ---------- Symbole für die vorhandenen Anleitungen ----------

SELECT pg_temp.symbole('Ein Foto mit WhatsApp schicken', 'ANDROID', ARRAY['chat-app','person','bueroklammer','bild','bild-auswahl','senden']);
SELECT pg_temp.symbole('Ein Foto mit WhatsApp schicken', 'IOS', ARRAY['chat-app','person','plus','bild','bild-auswahl','senden']);
SELECT pg_temp.symbole('Einen Videoanruf mit WhatsApp machen', 'ALLE', ARRAY['chat-app','person','videokamera','person','hoerer-rot']);
SELECT pg_temp.symbole('Eine Sprachnachricht mit WhatsApp schicken', 'ALLE', ARRAY['chat-app','finger','mikrofon','senden']);
SELECT pg_temp.symbole('Die Schrift größer machen', 'ANDROID', ARRAY['zahnrad','anzeige','schrift','regler']);
SELECT pg_temp.symbole('Die Schrift größer machen', 'IOS', ARRAY['zahnrad','anzeige','schrift','regler']);
SELECT pg_temp.symbole('Das Handy mit dem WLAN verbinden', 'ANDROID', ARRAY['zahnrad','weltkugel','wlan','router','schloss','haken']);
SELECT pg_temp.symbole('Das Handy mit dem WLAN verbinden', 'IOS', ARRAY['zahnrad','wlan','router','schloss','haken']);
SELECT pg_temp.symbole('Ein Bildschirmfoto machen', 'ANDROID', ARRAY['nachricht','seitentasten','bildschirmfoto']);
SELECT pg_temp.symbole('Ein Bildschirmfoto machen', 'IOS', ARRAY['nachricht','seitentasten','bildschirmfoto']);
SELECT pg_temp.symbole('Eine Telefonnummer speichern', 'ANDROID', ARRAY['kontakte','plus','tastatur','ziffernblock','haken']);
SELECT pg_temp.symbole('Eine Telefonnummer speichern', 'IOS', ARRAY['kontakte','plus','tastatur','ziffernblock','haken']);
SELECT pg_temp.symbole('Das Handy lauter stellen', 'ALLE', ARRAY['seitentasten','lautsprecher','person']);

-- ---------- Neue Anleitungen ----------

SELECT pg_temp.neu('Mit WhatsApp anrufen', 'WhatsApp', 'ALLE', 'telefonieren Anruf sprechen Hörer kostenlos', ARRAY[
    'chat-app|Öffnen Sie WhatsApp. Das Zeichen ist grün mit einem weißen Telefonhörer.',
    'person|Tippen Sie auf die Person, die Sie anrufen möchten.',
    'hoerer-gruen|Tippen Sie oben rechts auf den Telefonhörer.',
    'hoerer-rot|Zum Auflegen tippen Sie auf den roten Hörer.']);

SELECT pg_temp.neu('Eine Nachricht mit WhatsApp schreiben', 'WhatsApp', 'ALLE', 'Text schreiben tippen senden antworten Chat', ARRAY[
    'chat-app|Öffnen Sie WhatsApp.',
    'person|Tippen Sie auf die Person, der Sie schreiben möchten.',
    'tastatur|Tippen Sie unten in das Feld „Nachricht“. Schreiben Sie Ihren Text.',
    'senden|Tippen Sie rechts auf den Pfeil. Die Nachricht ist jetzt unterwegs.']);

SELECT pg_temp.neu('Ein Foto machen', 'Fotos', 'ALLE', 'Kamera fotografieren Bild aufnehmen knipsen', ARRAY[
    'kamera|Öffnen Sie die „Kamera“. Das Zeichen sieht aus wie ein Fotoapparat.',
    'bild|Halten Sie das Handy ruhig. Zeigen Sie auf das, was Sie fotografieren möchten.',
    'ausloeser|Tippen Sie unten auf den großen runden Knopf.',
    'bild-auswahl|Das Foto ist fertig. Sie finden es bei Ihren Fotos.']);

SELECT pg_temp.neu('Jemanden anrufen', 'Telefon', 'ALLE', 'telefonieren Anruf wählen Nummer Kontakt', ARRAY[
    'hoerer-gruen|Öffnen Sie „Telefon“. Das Zeichen ist ein Telefonhörer.',
    'kontakte|Tippen Sie auf „Kontakte“. Tippen Sie dann auf den Namen der Person.',
    'hoerer-gruen|Tippen Sie auf den Telefonhörer. Das Handy ruft jetzt an.',
    'hoerer-rot|Zum Auflegen tippen Sie auf den roten Hörer.']);

SELECT pg_temp.neu('Eine SMS schreiben', 'Telefon', 'ALLE', 'Nachricht Kurznachricht Text schicken senden', ARRAY[
    'nachricht|Öffnen Sie „Nachrichten“. Das Zeichen ist eine Sprechblase.',
    'plus|Tippen Sie auf das Zeichen für eine neue Nachricht. Es ist ein Stift oder ein Plus-Zeichen.',
    'person|Tippen Sie den Namen der Person ein. Tippen Sie dann auf den Vorschlag.',
    'tastatur|Tippen Sie unten in das Feld und schreiben Sie Ihre Nachricht.',
    'senden|Tippen Sie auf den Pfeil. Die Nachricht ist jetzt unterwegs.']);

SELECT pg_temp.neu('Den Bildschirm heller machen', 'Einstellungen', 'ANDROID', 'Helligkeit dunkel hell sehen Licht Display', ARRAY[
    'finger|Wischen Sie mit dem Finger vom oberen Rand des Bildschirms nach unten.',
    'sonne|Sie sehen einen Regler mit einer kleinen Sonne. Sehen Sie ihn nicht? Dann wischen Sie noch einmal nach unten.',
    'regler|Schieben Sie den Regler nach rechts. Der Bildschirm wird heller.']);

SELECT pg_temp.neu('Den Bildschirm heller machen', 'Einstellungen', 'IOS', 'Helligkeit dunkel hell sehen Licht Display', ARRAY[
    'finger|Wischen Sie von der oberen rechten Ecke nach unten. Hat Ihr iPhone unten einen runden Knopf? Dann wischen Sie vom unteren Rand nach oben.',
    'sonne|Sie sehen einen Regler mit einer kleinen Sonne.',
    'regler|Schieben Sie den Regler nach oben. Der Bildschirm wird heller.']);

SELECT pg_temp.neu('Eine App installieren', 'Apps', 'ANDROID', 'Programm herunterladen laden Play Store neue App', ARRAY[
    'einkaufstasche|Öffnen Sie den „Play Store“. Das Zeichen ist ein buntes Dreieck.',
    'lupe|Tippen Sie oben in das Suchfeld. Tippen Sie den Namen der App ein.',
    'bild-auswahl|Tippen Sie auf die richtige App. Sind Sie unsicher? Dann fragen Sie Ihre Familie.',
    'herunterladen|Tippen Sie auf „Installieren“.',
    'haken|Warten Sie, bis dort „Öffnen“ steht. Die App ist jetzt auf Ihrem Handy.']);

SELECT pg_temp.neu('Eine App installieren', 'Apps', 'IOS', 'Programm herunterladen laden App Store neue App', ARRAY[
    'einkaufstasche|Öffnen Sie den „App Store“. Das Zeichen ist blau mit einem weißen A.',
    'lupe|Tippen Sie unten rechts auf „Suchen“. Tippen Sie den Namen der App ein.',
    'bild-auswahl|Tippen Sie auf die richtige App. Sind Sie unsicher? Dann fragen Sie Ihre Familie.',
    'herunterladen|Tippen Sie auf „Laden“. Vielleicht fragt das iPhone nach Ihrem Passwort oder Ihrem Gesicht.',
    'haken|Warten Sie, bis dort „Öffnen“ steht. Die App ist jetzt auf Ihrem iPhone.']);

SELECT pg_temp.neu('Die Taschenlampe einschalten', 'Einstellungen', 'ANDROID', 'Licht Lampe leuchten dunkel', ARRAY[
    'finger|Wischen Sie mit dem Finger vom oberen Rand des Bildschirms nach unten.',
    'taschenlampe|Tippen Sie auf „Taschenlampe“. Das Licht auf der Rückseite geht an.',
    'taschenlampe|Zum Ausschalten tippen Sie noch einmal darauf.']);

SELECT pg_temp.neu('Die Taschenlampe einschalten', 'Einstellungen', 'IOS', 'Licht Lampe leuchten dunkel', ARRAY[
    'finger|Wischen Sie von der oberen rechten Ecke nach unten. Hat Ihr iPhone unten einen runden Knopf? Dann wischen Sie vom unteren Rand nach oben.',
    'taschenlampe|Tippen Sie auf das Zeichen mit der Taschenlampe. Das Licht auf der Rückseite geht an.',
    'taschenlampe|Zum Ausschalten tippen Sie noch einmal darauf.']);

SELECT pg_temp.neu('Einen Wecker stellen', 'Einstellungen', 'ALLE', 'Uhr wecken aufstehen Alarm Zeit', ARRAY[
    'uhr-app|Öffnen Sie die App „Uhr“.',
    'wecker|Tippen Sie auf „Wecker“.',
    'plus|Tippen Sie auf das Plus-Zeichen.',
    'wecker|Stellen Sie die Uhrzeit ein, zu der Sie geweckt werden möchten.',
    'haken|Tippen Sie auf „Speichern“. Auf dem iPhone heißt es „Sichern“.']);

SELECT pg_temp.neu('Eine unbekannte Nummer blockieren', 'Telefon', 'ANDROID', 'sperren Anrufer Werbung belästigen nervig blocken', ARRAY[
    'hoerer-gruen|Öffnen Sie „Telefon“.',
    'ziffernblock|Tippen Sie auf „Anrufliste“. Bei manchen Handys heißt es „Letzte“.',
    'finger|Legen Sie den Finger lange auf die Nummer, die Sie stört.',
    'sperren|Tippen Sie auf „Blockieren“. Bei manchen Handys heißt es „Nummer sperren“.']);

SELECT pg_temp.neu('Eine unbekannte Nummer blockieren', 'Telefon', 'IOS', 'sperren Anrufer Werbung belästigen nervig blocken', ARRAY[
    'hoerer-gruen|Öffnen Sie „Telefon“.',
    'ziffernblock|Tippen Sie unten auf „Anrufliste“.',
    'person|Tippen Sie neben der Nummer auf das kleine „i“ im Kreis.',
    'sperren|Gehen Sie ganz nach unten. Tippen Sie auf „Anrufer blockieren“.']);

SELECT pg_temp.neu('Im Internet etwas suchen', 'Internet', 'ANDROID', 'googeln Google suchen finden Browser Chrome Seite', ARRAY[
    'weltkugel|Öffnen Sie „Chrome“. Das Zeichen ist ein bunter Kreis.',
    'lupe|Tippen Sie oben in das Feld.',
    'tastatur|Schreiben Sie, was Sie suchen. Zum Beispiel: Wetter morgen.',
    'haken|Tippen Sie auf der Tastatur unten rechts auf die Lupe oder den Pfeil.']);

SELECT pg_temp.neu('Im Internet etwas suchen', 'Internet', 'IOS', 'googeln Google suchen finden Browser Safari Seite', ARRAY[
    'weltkugel|Öffnen Sie „Safari“. Das Zeichen ist ein blauer Kompass.',
    'lupe|Tippen Sie in das Feld mit der Lupe. Es ist unten oder oben am Bildschirm.',
    'tastatur|Schreiben Sie, was Sie suchen. Zum Beispiel: Wetter morgen.',
    'haken|Tippen Sie auf der Tastatur unten rechts auf „Öffnen“.']);

SELECT pg_temp.neu('Das Handy neu starten', 'Einstellungen', 'ANDROID', 'ausschalten einschalten hängt langsam reagiert nicht Neustart', ARRAY[
    'ein-aus|Halten Sie die Ein-/Aus-Taste an der Seite lange gedrückt. Passiert nichts? Dann halten Sie zusätzlich die Leiser-Taste gedrückt.',
    'ein-aus|Tippen Sie auf „Neu starten“.',
    'haken|Warten Sie, bis das Handy wieder an ist. Das dauert etwa eine Minute.']);

SELECT pg_temp.neu('Das Handy neu starten', 'Einstellungen', 'IOS', 'ausschalten einschalten hängt langsam reagiert nicht Neustart', ARRAY[
    'seitentasten|Halten Sie die Seitentaste und eine Lautstärke-Taste gedrückt. Hat Ihr iPhone unten einen runden Knopf? Dann halten Sie nur die Seitentaste gedrückt.',
    'regler|Schieben Sie den Regler „Ausschalten“ nach rechts.',
    'ein-aus|Warten Sie eine halbe Minute. Halten Sie dann die Seitentaste gedrückt, bis ein Apfel erscheint.']);

SELECT pg_temp.neu('Einen QR-Code scannen', 'Fotos', 'ALLE', 'Kamera Code Quadrat einlesen Link', ARRAY[
    'kamera|Öffnen Sie die „Kamera“.',
    'qr|Halten Sie das Handy so, dass der QR-Code ganz im Bild ist.',
    'finger|Auf dem Bildschirm erscheint ein Link. Tippen Sie darauf. Scannen Sie nur Codes, denen Sie vertrauen.']);

SELECT pg_temp.neu('Den Notruf 112 anrufen', 'Telefon', 'ALLE', 'Notfall Hilfe Rettung Feuerwehr Krankenwagen Unfall', ARRAY[
    'hoerer-gruen|Öffnen Sie „Telefon“.',
    'ziffernblock|Tippen Sie auf das Tastenfeld. Wählen Sie 1 1 2.',
    'notruf|Tippen Sie auf den grünen Hörer. Der Notruf ist kostenlos.',
    'person|Sagen Sie, wo Sie sind und was passiert ist. Legen Sie erst auf, wenn man es Ihnen sagt.']);

SELECT pg_temp.neu('Den Flugmodus ausschalten', 'Einstellungen', 'ANDROID', 'kein Netz kein Empfang kein Internet Flugzeug Telefon geht nicht', ARRAY[
    'flugzeug|Sehen Sie oben am Bildschirm ein kleines Flugzeug? Dann ist der Flugmodus an. Telefon und Internet gehen dann nicht.',
    'finger|Wischen Sie mit dem Finger vom oberen Rand des Bildschirms nach unten.',
    'flugzeug|Tippen Sie auf das Flugzeug. Der Flugmodus ist jetzt aus.']);

SELECT pg_temp.neu('Den Flugmodus ausschalten', 'Einstellungen', 'IOS', 'kein Netz kein Empfang kein Internet Flugzeug Telefon geht nicht', ARRAY[
    'flugzeug|Sehen Sie oben am Bildschirm ein kleines Flugzeug? Dann ist der Flugmodus an. Telefon und Internet gehen dann nicht.',
    'finger|Wischen Sie von der oberen rechten Ecke nach unten. Hat Ihr iPhone unten einen runden Knopf? Dann wischen Sie vom unteren Rand nach oben.',
    'flugzeug|Tippen Sie auf das orange Flugzeug. Es wird grau. Der Flugmodus ist jetzt aus.']);
