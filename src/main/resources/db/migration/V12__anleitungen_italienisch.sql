-- BEISPIEL-INHALTE: alle Anleitungen auf Italienisch. Noch nicht an echten Geräten geprüft.
-- Jeder Schritt hat die Form 'symbol|Text'. Im Text steht ’ statt ' (kein Maskieren nötig).

CREATE OR REPLACE FUNCTION pg_temp.neu(p_titel text, p_kategorie text, p_plattform text, p_stichworte text,
                            p_schritte text[]) RETURNS void AS $$
DECLARE
    neue_id uuid := gen_random_uuid();
BEGIN
    INSERT INTO anleitung (id, titel, kategorie, plattform, quelle, stichworte, sprache)
    VALUES (neue_id, p_titel, p_kategorie, p_plattform, 'SYSTEM', p_stichworte, 'it');
    INSERT INTO anleitung_schritt (anleitung_id, nummer, text, bild_pfad)
    SELECT neue_id, s.nummer, substring(s.zeile from position('|' in s.zeile) + 1), split_part(s.zeile, '|', 1)
    FROM unnest(p_schritte) WITH ORDINALITY AS s(zeile, nummer);
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.neu('Inviare una foto con WhatsApp', 'WhatsApp', 'ANDROID', 'immagine mandare spedire foto galleria condividere', ARRAY[
    'chat-app|Apra WhatsApp. L’icona è verde con una cornetta bianca.',
    'person|Tocchi la persona a cui vuole inviare la foto.',
    'bueroklammer|Tocchi la graffetta in basso a destra.',
    'bild|Tocchi «Galleria».',
    'bild-auswahl|Tocchi la foto che vuole inviare.',
    'senden|Tocchi la freccia verde in basso a destra. La foto è in viaggio.']);

SELECT pg_temp.neu('Inviare una foto con WhatsApp', 'WhatsApp', 'IOS', 'immagine mandare spedire foto libreria condividere', ARRAY[
    'chat-app|Apra WhatsApp. L’icona è verde con una cornetta bianca.',
    'person|Tocchi la persona a cui vuole inviare la foto.',
    'plus|Tocchi il segno più in basso a sinistra.',
    'bild|Tocchi «Foto».',
    'bild-auswahl|Tocchi la foto che vuole inviare.',
    'senden|Tocchi la freccia in basso a destra. La foto è in viaggio.']);

SELECT pg_temp.neu('Fare una videochiamata con WhatsApp', 'WhatsApp', 'ALLE', 'video chiamare vedere viso videocamera', ARRAY[
    'chat-app|Apra WhatsApp.',
    'person|Tocchi la persona che vuole chiamare.',
    'videokamera|Tocchi la piccola videocamera in alto a destra.',
    'person|Aspetti che la persona risponda. Tenga il telefono in modo che si veda il Suo viso.',
    'hoerer-rot|Per chiudere, tocchi la cornetta rossa.']);

SELECT pg_temp.neu('Inviare un messaggio vocale con WhatsApp', 'WhatsApp', 'ALLE', 'parlare registrare microfono audio vocale', ARRAY[
    'chat-app|Apra WhatsApp e tocchi la persona.',
    'finger|In basso a destra vede un microfono. Ci metta sopra il dito e lo tenga lì.',
    'mikrofon|Ora dica il Suo messaggio.',
    'senden|Tolga il dito. Il messaggio viene inviato subito.']);

SELECT pg_temp.neu('Ingrandire le scritte', 'Impostazioni', 'ANDROID', 'carattere dimensione leggere piccolo grande testo schermo', ARRAY[
    'zahnrad|Apra «Impostazioni». L’icona è una ruota dentata.',
    'anzeige|Tocchi «Schermo». Su alcuni telefoni si chiama «Display».',
    'schrift|Tocchi «Dimensioni carattere».',
    'regler|Sposti il cursore verso destra finché riesce a leggere bene.']);

SELECT pg_temp.neu('Ingrandire le scritte', 'Impostazioni', 'IOS', 'carattere dimensione leggere piccolo grande testo schermo', ARRAY[
    'zahnrad|Apra «Impostazioni». L’icona è una ruota dentata grigia.',
    'anzeige|Tocchi «Schermo e luminosità».',
    'schrift|Tocchi «Dimensioni testo».',
    'regler|Sposti il cursore verso destra finché riesce a leggere bene.']);

SELECT pg_temp.neu('Collegare il telefono al Wi-Fi', 'Internet', 'ANDROID', 'internet wifi rete router password collegare connessione', ARRAY[
    'zahnrad|Apra «Impostazioni». L’icona è una ruota dentata.',
    'weltkugel|Tocchi «Rete e Internet». Su alcuni telefoni si chiama «Connessioni».',
    'wlan|Tocchi «Wi-Fi». Su alcuni telefoni si chiama «Internet».',
    'router|Tocchi il nome del Suo Wi-Fi. Di solito è scritto dietro al router.',
    'schloss|Scriva la password del Wi-Fi. Anche questa di solito è scritta sul router.',
    'haken|Tocchi «Connetti».']);

SELECT pg_temp.neu('Collegare il telefono al Wi-Fi', 'Internet', 'IOS', 'internet wifi rete router password collegare connessione', ARRAY[
    'zahnrad|Apra «Impostazioni». L’icona è una ruota dentata grigia.',
    'wlan|Tocchi «Wi-Fi».',
    'router|Tocchi il nome del Suo Wi-Fi. Di solito è scritto dietro al router.',
    'schloss|Scriva la password del Wi-Fi. Anche questa di solito è scritta sul router.',
    'haken|Tocchi «Accedi» in alto a destra.']);

SELECT pg_temp.neu('Fare una schermata', 'Foto', 'ANDROID', 'screenshot schermo fotografare catturare', ARRAY[
    'nachricht|Apra quello che vuole fotografare, per esempio un messaggio.',
    'seitentasten|Prema insieme, per un attimo, il tasto di accensione e il tasto per abbassare il volume.',
    'bildschirmfoto|Lo schermo lampeggia per un attimo. La schermata ora è tra le Sue foto.']);

SELECT pg_temp.neu('Fare una schermata', 'Foto', 'IOS', 'screenshot schermo fotografare catturare', ARRAY[
    'nachricht|Apra quello che vuole fotografare, per esempio un messaggio.',
    'seitentasten|Prema insieme, per un attimo, il tasto laterale e il tasto per alzare il volume. Il Suo iPhone ha un tasto rotondo in basso? Allora prema il tasto laterale e il tasto rotondo.',
    'bildschirmfoto|Lo schermo lampeggia per un attimo. La schermata ora è tra le Sue foto.']);

SELECT pg_temp.neu('Salvare un numero di telefono', 'Telefono', 'ANDROID', 'contatto aggiungere nuovo numero rubrica', ARRAY[
    'kontakte|Apra «Contatti».',
    'plus|Tocchi il segno più.',
    'tastatur|Scriva il nome.',
    'ziffernblock|Scriva il numero di telefono.',
    'haken|Tocchi «Salva».']);

SELECT pg_temp.neu('Salvare un numero di telefono', 'Telefono', 'IOS', 'contatto aggiungere nuovo numero rubrica', ARRAY[
    'kontakte|Apra «Contatti».',
    'plus|Tocchi il segno più in alto a destra.',
    'tastatur|Scriva il nome.',
    'ziffernblock|Tocchi «aggiungi telefono» e scriva il numero.',
    'haken|Tocchi «Fine» in alto a destra.']);

SELECT pg_temp.neu('Alzare il volume del telefono', 'Impostazioni', 'ALLE', 'volume suono basso alto suoneria sentire', ARRAY[
    'seitentasten|Sul lato del telefono ci sono due tasti per il volume.',
    'lautsprecher|Prema più volte il tasto in alto. Sullo schermo vede una barra che si allunga.',
    'person|Non sente ancora la suoneria? Allora chieda alla Sua famiglia. Forse il telefono è in modalità silenziosa.']);

SELECT pg_temp.neu('Telefonare con WhatsApp', 'WhatsApp', 'ALLE', 'chiamare chiamata parlare cornetta gratis', ARRAY[
    'chat-app|Apra WhatsApp. L’icona è verde con una cornetta bianca.',
    'person|Tocchi la persona che vuole chiamare.',
    'hoerer-gruen|Tocchi la cornetta in alto a destra.',
    'hoerer-rot|Per chiudere, tocchi la cornetta rossa.']);

SELECT pg_temp.neu('Scrivere un messaggio con WhatsApp', 'WhatsApp', 'ALLE', 'testo scrivere inviare rispondere chat', ARRAY[
    'chat-app|Apra WhatsApp.',
    'person|Tocchi la persona a cui vuole scrivere.',
    'tastatur|Tocchi il campo «Messaggio» in basso. Scriva il Suo testo.',
    'senden|Tocchi la freccia a destra. Il messaggio è in viaggio.']);

SELECT pg_temp.neu('Fare una foto', 'Foto', 'ALLE', 'fotocamera fotografare immagine scattare', ARRAY[
    'kamera|Apra la «Fotocamera». L’icona sembra una macchina fotografica.',
    'bild|Tenga fermo il telefono. Lo punti verso quello che vuole fotografare.',
    'ausloeser|Tocchi il grande tasto rotondo in basso.',
    'bild-auswahl|La foto è fatta. La trova tra le Sue foto.']);

SELECT pg_temp.neu('Telefonare a qualcuno', 'Telefono', 'ALLE', 'chiamare chiamata comporre numero contatto', ARRAY[
    'hoerer-gruen|Apra «Telefono». L’icona è una cornetta.',
    'kontakte|Tocchi «Contatti». Poi tocchi il nome della persona.',
    'hoerer-gruen|Tocchi la cornetta. Il telefono ora sta chiamando.',
    'hoerer-rot|Per chiudere, tocchi la cornetta rossa.']);

SELECT pg_temp.neu('Scrivere un SMS', 'Telefono', 'ALLE', 'messaggio messaggino testo inviare mandare', ARRAY[
    'nachricht|Apra «Messaggi». L’icona è un fumetto.',
    'plus|Tocchi il segno per un nuovo messaggio. È una matita oppure un segno più.',
    'person|Scriva il nome della persona. Poi tocchi il nome che Le viene proposto.',
    'tastatur|Tocchi il campo in basso e scriva il Suo messaggio.',
    'senden|Tocchi la freccia. Il messaggio è in viaggio.']);

SELECT pg_temp.neu('Rendere lo schermo più luminoso', 'Impostazioni', 'ANDROID', 'luminosità scuro chiaro vedere luce schermo', ARRAY[
    'finger|Scorra con il dito dal bordo in alto dello schermo verso il basso.',
    'sonne|Vede un cursore con un piccolo sole. Non lo vede? Allora scorra ancora una volta verso il basso.',
    'regler|Sposti il cursore verso destra. Lo schermo diventa più luminoso.']);

SELECT pg_temp.neu('Rendere lo schermo più luminoso', 'Impostazioni', 'IOS', 'luminosità scuro chiaro vedere luce schermo', ARRAY[
    'finger|Scorra dall’angolo in alto a destra verso il basso. Il Suo iPhone ha un tasto rotondo in basso? Allora scorra dal bordo in basso verso l’alto.',
    'sonne|Vede un cursore con un piccolo sole.',
    'regler|Sposti il cursore verso l’alto. Lo schermo diventa più luminoso.']);

SELECT pg_temp.neu('Installare un’app', 'App', 'ANDROID', 'programma scaricare play store nuova app applicazione', ARRAY[
    'einkaufstasche|Apra il «Play Store». L’icona è un triangolo colorato.',
    'lupe|Tocchi il campo di ricerca in alto. Scriva il nome dell’app.',
    'bild-auswahl|Tocchi l’app giusta. Non è sicuro? Allora chieda alla Sua famiglia.',
    'herunterladen|Tocchi «Installa».',
    'haken|Aspetti finché compare «Apri». L’app ora è sul Suo telefono.']);

SELECT pg_temp.neu('Installare un’app', 'App', 'IOS', 'programma scaricare app store nuova app applicazione', ARRAY[
    'einkaufstasche|Apra l’«App Store». L’icona è blu con una A bianca.',
    'lupe|Tocchi «Cerca» in basso a destra. Scriva il nome dell’app.',
    'bild-auswahl|Tocchi l’app giusta. Non è sicuro? Allora chieda alla Sua famiglia.',
    'herunterladen|Tocchi «Ottieni». Forse l’iPhone chiede la Sua password o il Suo viso.',
    'haken|Aspetti finché compare «Apri». L’app ora è sul Suo iPhone.']);

SELECT pg_temp.neu('Accendere la torcia', 'Impostazioni', 'ANDROID', 'luce lampada illuminare buio pila', ARRAY[
    'finger|Scorra con il dito dal bordo in alto dello schermo verso il basso.',
    'taschenlampe|Tocchi «Torcia». La luce sul retro si accende.',
    'taschenlampe|Per spegnerla, tocchi ancora una volta.']);

SELECT pg_temp.neu('Accendere la torcia', 'Impostazioni', 'IOS', 'luce lampada illuminare buio pila', ARRAY[
    'finger|Scorra dall’angolo in alto a destra verso il basso. Il Suo iPhone ha un tasto rotondo in basso? Allora scorra dal bordo in basso verso l’alto.',
    'taschenlampe|Tocchi l’icona con la torcia. La luce sul retro si accende.',
    'taschenlampe|Per spegnerla, tocchi ancora una volta.']);

SELECT pg_temp.neu('Impostare una sveglia', 'Impostazioni', 'ALLE', 'orologio svegliare alzarsi allarme ora mattina', ARRAY[
    'uhr-app|Apra l’app «Orologio».',
    'wecker|Tocchi «Sveglia».',
    'plus|Tocchi il segno più.',
    'wecker|Scelga l’ora a cui vuole essere svegliato.',
    'haken|Tocchi «Salva».']);

SELECT pg_temp.neu('Bloccare un numero sconosciuto', 'Telefono', 'ANDROID', 'bloccare chiamate pubblicità disturbo fastidio spam', ARRAY[
    'hoerer-gruen|Apra «Telefono».',
    'ziffernblock|Tocchi «Recenti».',
    'finger|Tenga il dito a lungo sul numero che La disturba.',
    'sperren|Tocchi «Blocca». Su alcuni telefoni si chiama «Blocca numero».']);

SELECT pg_temp.neu('Bloccare un numero sconosciuto', 'Telefono', 'IOS', 'bloccare chiamate pubblicità disturbo fastidio spam', ARRAY[
    'hoerer-gruen|Apra «Telefono».',
    'ziffernblock|Tocchi «Recenti» in basso.',
    'person|Accanto al numero, tocchi la piccola «i» nel cerchio.',
    'sperren|Vada fino in fondo. Tocchi «Blocca contatto».']);

SELECT pg_temp.neu('Cercare qualcosa su internet', 'Internet', 'ANDROID', 'google cercare trovare browser chrome sito', ARRAY[
    'weltkugel|Apra «Chrome». L’icona è un cerchio colorato.',
    'lupe|Tocchi il campo in alto.',
    'tastatur|Scriva quello che cerca. Per esempio: meteo domani.',
    'haken|Sulla tastiera, tocchi la lente o la freccia in basso a destra.']);

SELECT pg_temp.neu('Cercare qualcosa su internet', 'Internet', 'IOS', 'google cercare trovare browser safari sito', ARRAY[
    'weltkugel|Apra «Safari». L’icona è una bussola blu.',
    'lupe|Tocchi il campo con la lente. Si trova in basso oppure in alto sullo schermo.',
    'tastatur|Scriva quello che cerca. Per esempio: meteo domani.',
    'haken|Sulla tastiera, tocchi «vai» in basso a destra.']);

SELECT pg_temp.neu('Riavviare il telefono', 'Impostazioni', 'ANDROID', 'spegnere accendere bloccato lento non risponde riavvio', ARRAY[
    'ein-aus|Tenga premuto a lungo il tasto di accensione sul lato. Non succede niente? Allora tenga premuto anche il tasto per abbassare il volume.',
    'ein-aus|Tocchi «Riavvia».',
    'haken|Aspetti che il telefono si riaccenda. Ci vuole circa un minuto.']);

SELECT pg_temp.neu('Riavviare il telefono', 'Impostazioni', 'IOS', 'spegnere accendere bloccato lento non risponde riavvio', ARRAY[
    'seitentasten|Tenga premuti il tasto laterale e un tasto del volume. Il Suo iPhone ha un tasto rotondo in basso? Allora tenga premuto solo il tasto laterale.',
    'regler|Sposti verso destra il cursore per spegnere.',
    'ein-aus|Aspetti mezzo minuto. Poi tenga premuto il tasto laterale finché compare una mela.']);

SELECT pg_temp.neu('Leggere un codice QR', 'Foto', 'ALLE', 'fotocamera codice quadrato scansionare inquadrare link', ARRAY[
    'kamera|Apra la «Fotocamera».',
    'qr|Tenga il telefono in modo che tutto il codice QR sia nell’immagine.',
    'finger|Sullo schermo compare un link. Lo tocchi. Inquadri solo codici di cui si fida.']);

SELECT pg_temp.neu('Chiamare il numero di emergenza 112', 'Telefono', 'ALLE', 'emergenza aiuto ambulanza pompieri incidente soccorso', ARRAY[
    'hoerer-gruen|Apra «Telefono».',
    'ziffernblock|Tocchi il tastierino. Componga 1 1 2.',
    'notruf|Tocchi la cornetta verde. La chiamata di emergenza è gratuita. Il 112 funziona in tutta l’Unione europea.',
    'person|Dica dove si trova e che cosa è successo. Chiuda solo quando glielo dicono.']);

SELECT pg_temp.neu('Spegnere la modalità aereo', 'Impostazioni', 'ANDROID', 'aereo nessuna rete nessun segnale niente internet telefono non funziona', ARRAY[
    'flugzeug|Vede un piccolo aereo in alto sullo schermo? Allora la modalità aereo è accesa. Telefono e internet non funzionano.',
    'finger|Scorra con il dito dal bordo in alto dello schermo verso il basso.',
    'flugzeug|Tocchi l’aereo. La modalità aereo ora è spenta.']);

SELECT pg_temp.neu('Spegnere la modalità aereo', 'Impostazioni', 'IOS', 'aereo uso in aereo nessuna rete nessun segnale niente internet telefono non funziona', ARRAY[
    'flugzeug|Vede un piccolo aereo in alto sullo schermo? Allora la modalità aereo è accesa. Telefono e internet non funzionano.',
    'finger|Scorra dall’angolo in alto a destra verso il basso. Il Suo iPhone ha un tasto rotondo in basso? Allora scorra dal bordo in basso verso l’alto.',
    'flugzeug|Tocchi l’aereo arancione. Diventa grigio. La modalità aereo ora è spenta.']);
