-- BEISPIEL-INHALTE: zwei Anleitungen rund um "Teilen an HalloKlick", in drei Sprachen.
-- Das Teilen geht nur auf Android und nur, wenn HalloKlick installiert ist.
-- Jeder Schritt hat die Form 'symbol|Text'. Im Text steht ’ statt '.

CREATE OR REPLACE FUNCTION pg_temp.neu_in(p_sprache text, p_titel text, p_kategorie text, p_plattform text,
                                          p_stichworte text, p_schritte text[]) RETURNS void AS $$
DECLARE
    neue_id uuid := gen_random_uuid();
BEGIN
    INSERT INTO anleitung (id, titel, kategorie, plattform, quelle, stichworte, sprache)
    VALUES (neue_id, p_titel, p_kategorie, p_plattform, 'SYSTEM', p_stichworte, p_sprache);
    INSERT INTO anleitung_schritt (anleitung_id, nummer, text, bild_pfad)
    SELECT neue_id, s.nummer, substring(s.zeile from position('|' in s.zeile) + 1), split_part(s.zeile, '|', 1)
    FROM unnest(p_schritte) WITH ORDINALITY AS s(zeile, nummer);
END;
$$ LANGUAGE plpgsql;

-- ---------- Deutsch ----------

SELECT pg_temp.neu_in('de', 'HalloKlick auf dem Handy installieren', 'HalloKlick', 'ANDROID',
    'App Startbildschirm hinzufügen installieren Symbol HalloKlick', ARRAY[
    'weltkugel|Öffnen Sie HalloKlick in „Chrome“. Das Zeichen von Chrome ist ein bunter Kreis.',
    'finger|Tippen Sie oben rechts auf die drei Punkte.',
    'herunterladen|Tippen Sie auf „Zum Startbildschirm hinzufügen“. Bei manchen Handys heißt es „App installieren“.',
    'haken|Tippen Sie auf „Installieren“. HalloKlick liegt jetzt bei Ihren anderen Apps.']);

SELECT pg_temp.neu_in('de', 'Eine Nachricht aus WhatsApp mit HalloKlick prüfen', 'HalloKlick', 'ANDROID',
    'teilen prüfen Betrug echt Nachricht weitergeben HalloKlick ohne kopieren', ARRAY[
    'chat-app|Öffnen Sie WhatsApp. Tippen Sie auf die Unterhaltung mit der Nachricht.',
    'finger|Legen Sie den Finger lange auf die Nachricht, bis sie farbig markiert ist.',
    'nachricht|Tippen Sie oben rechts auf die drei Punkte. Tippen Sie dann auf „Teilen“.',
    'haken|Tippen Sie auf „HalloKlick“. Die Nachricht wird sofort geprüft. Sehen Sie HalloKlick nicht? Dann muss es erst installiert werden.']);

-- ---------- Englisch ----------

SELECT pg_temp.neu_in('en', 'Install HalloKlick on the phone', 'HalloKlick', 'ANDROID',
    'app home screen add install icon halloklick', ARRAY[
    'weltkugel|Open HalloKlick in “Chrome”. The Chrome icon is a colourful circle.',
    'finger|Tap the three dots at the top right.',
    'herunterladen|Tap “Add to Home screen”. On some phones it is called “Install app”.',
    'haken|Tap “Install”. HalloKlick is now with your other apps.']);

SELECT pg_temp.neu_in('en', 'Check a WhatsApp message with HalloKlick', 'HalloKlick', 'ANDROID',
    'share check scam real message forward halloklick without copying', ARRAY[
    'chat-app|Open WhatsApp. Tap the chat with the message.',
    'finger|Keep your finger on the message until it is highlighted.',
    'nachricht|Tap the three dots at the top right. Then tap “Share”.',
    'haken|Tap “HalloKlick”. The message is checked straight away. Can you not see HalloKlick? Then it has to be installed first.']);

-- ---------- Italienisch ----------

SELECT pg_temp.neu_in('it', 'Installare HalloKlick sul telefono', 'HalloKlick', 'ANDROID',
    'app schermata home aggiungere installare icona halloklick', ARRAY[
    'weltkugel|Apra HalloKlick in «Chrome». L’icona di Chrome è un cerchio colorato.',
    'finger|Tocchi i tre puntini in alto a destra.',
    'herunterladen|Tocchi «Aggiungi a schermata Home». Su alcuni telefoni si chiama «Installa app».',
    'haken|Tocchi «Installa». HalloKlick ora è insieme alle Sue altre app.']);

SELECT pg_temp.neu_in('it', 'Controllare un messaggio di WhatsApp con HalloKlick', 'HalloKlick', 'ANDROID',
    'condividere controllare truffa vero messaggio inoltrare halloklick senza copiare', ARRAY[
    'chat-app|Apra WhatsApp. Tocchi la chat con il messaggio.',
    'finger|Tenga il dito sul messaggio finché viene evidenziato.',
    'nachricht|Tocchi i tre puntini in alto a destra. Poi tocchi «Condividi».',
    'haken|Tocchi «HalloKlick». Il messaggio viene controllato subito. Non vede HalloKlick? Allora deve prima essere installato.']);
