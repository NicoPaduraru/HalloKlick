-- BEISPIEL-INHALTE: alle Anleitungen auf Englisch. Noch nicht an echten Geräten geprüft.
-- Jeder Schritt hat die Form 'symbol|Text'. Im Text steht ’ statt ' (kein Maskieren nötig).

CREATE OR REPLACE FUNCTION pg_temp.neu(p_titel text, p_kategorie text, p_plattform text, p_stichworte text,
                            p_schritte text[]) RETURNS void AS $$
DECLARE
    neue_id uuid := gen_random_uuid();
BEGIN
    INSERT INTO anleitung (id, titel, kategorie, plattform, quelle, stichworte, sprache)
    VALUES (neue_id, p_titel, p_kategorie, p_plattform, 'SYSTEM', p_stichworte, 'en');
    INSERT INTO anleitung_schritt (anleitung_id, nummer, text, bild_pfad)
    SELECT neue_id, s.nummer, substring(s.zeile from position('|' in s.zeile) + 1), split_part(s.zeile, '|', 1)
    FROM unnest(p_schritte) WITH ORDINALITY AS s(zeile, nummer);
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.neu('Send a photo with WhatsApp', 'WhatsApp', 'ANDROID', 'picture image send share photos gallery', ARRAY[
    'chat-app|Open WhatsApp. The icon is green with a white telephone.',
    'person|Tap the person you want to send the photo to.',
    'bueroklammer|Tap the paper clip at the bottom right.',
    'bild|Tap “Gallery”.',
    'bild-auswahl|Tap the photo you want to send.',
    'senden|Tap the green arrow at the bottom right. The photo is now on its way.']);

SELECT pg_temp.neu('Send a photo with WhatsApp', 'WhatsApp', 'IOS', 'picture image send share photos library', ARRAY[
    'chat-app|Open WhatsApp. The icon is green with a white telephone.',
    'person|Tap the person you want to send the photo to.',
    'plus|Tap the plus sign at the bottom left.',
    'bild|Tap “Photos”.',
    'bild-auswahl|Tap the photo you want to send.',
    'senden|Tap the arrow at the bottom right. The photo is now on its way.']);

SELECT pg_temp.neu('Make a video call with WhatsApp', 'WhatsApp', 'ALLE', 'video call see face camera', ARRAY[
    'chat-app|Open WhatsApp.',
    'person|Tap the person you want to call.',
    'videokamera|Tap the small camera at the top right.',
    'person|Wait until the person answers. Hold the phone so that your face can be seen.',
    'hoerer-rot|To hang up, tap the red telephone.']);

SELECT pg_temp.neu('Send a voice message with WhatsApp', 'WhatsApp', 'ALLE', 'speak record microphone audio voice note', ARRAY[
    'chat-app|Open WhatsApp and tap the person.',
    'finger|At the bottom right you see a microphone. Put your finger on it and keep it there.',
    'mikrofon|Now say your message.',
    'senden|Take your finger away. The message is sent straight away.']);

SELECT pg_temp.neu('Make the text bigger', 'Settings', 'ANDROID', 'font size read small large letters display', ARRAY[
    'zahnrad|Open “Settings”. The icon is a cog wheel.',
    'anzeige|Tap “Display”.',
    'schrift|Tap “Font size”. On some phones it is called “Display size and text”.',
    'regler|Move the slider to the right until you can read the text well.']);

SELECT pg_temp.neu('Make the text bigger', 'Settings', 'IOS', 'font size read small large letters display', ARRAY[
    'zahnrad|Open “Settings”. The icon is a grey cog wheel.',
    'anzeige|Tap “Display & Brightness”.',
    'schrift|Tap “Text Size”.',
    'regler|Move the slider to the right until you can read the text well.']);

SELECT pg_temp.neu('Connect the phone to Wi-Fi', 'Internet', 'ANDROID', 'internet wifi wlan network router password connect', ARRAY[
    'zahnrad|Open “Settings”. The icon is a cog wheel.',
    'weltkugel|Tap “Network & internet”. On some phones it is called “Connections”.',
    'wlan|Tap “Wi-Fi”. On some phones it is called “Internet”.',
    'router|Tap the name of your Wi-Fi. It is usually printed on the back of your router.',
    'schloss|Type in the Wi-Fi password. That is usually printed on the router too.',
    'haken|Tap “Connect”.']);

SELECT pg_temp.neu('Connect the phone to Wi-Fi', 'Internet', 'IOS', 'internet wifi wlan network router password connect', ARRAY[
    'zahnrad|Open “Settings”. The icon is a grey cog wheel.',
    'wlan|Tap “Wi-Fi”.',
    'router|Tap the name of your Wi-Fi. It is usually printed on the back of your router.',
    'schloss|Type in the Wi-Fi password. That is usually printed on the router too.',
    'haken|Tap “Join” at the top right.']);

SELECT pg_temp.neu('Take a screenshot', 'Photos', 'ANDROID', 'screen capture picture of the screen', ARRAY[
    'nachricht|Open what you want to capture, for example a message.',
    'seitentasten|Press the power button and the volume down button briefly at the same time.',
    'bildschirmfoto|The screen flashes briefly. The screenshot is now with your photos.']);

SELECT pg_temp.neu('Take a screenshot', 'Photos', 'IOS', 'screen capture picture of the screen', ARRAY[
    'nachricht|Open what you want to capture, for example a message.',
    'seitentasten|Press the side button and the volume up button briefly at the same time. Does your iPhone have a round button at the bottom? Then press the side button and the round button.',
    'bildschirmfoto|The screen flashes briefly. The screenshot is now with your photos.']);

SELECT pg_temp.neu('Save a phone number', 'Phone', 'ANDROID', 'contact add new number address book', ARRAY[
    'kontakte|Open “Contacts”.',
    'plus|Tap the plus sign.',
    'tastatur|Type in the name.',
    'ziffernblock|Type in the phone number.',
    'haken|Tap “Save”.']);

SELECT pg_temp.neu('Save a phone number', 'Phone', 'IOS', 'contact add new number address book', ARRAY[
    'kontakte|Open “Contacts”.',
    'plus|Tap the plus sign at the top right.',
    'tastatur|Type in the name.',
    'ziffernblock|Tap “add phone” and type in the number.',
    'haken|Tap “Done” at the top right.']);

SELECT pg_temp.neu('Make the phone louder', 'Settings', 'ALLE', 'volume sound quiet loud ringtone hear', ARRAY[
    'seitentasten|On the side of your phone there are two buttons for the volume.',
    'lautsprecher|Press the upper button several times. On the screen you see a bar that gets longer.',
    'person|Still cannot hear the ringtone? Then ask your family. Perhaps the phone is set to silent.']);

SELECT pg_temp.neu('Call someone with WhatsApp', 'WhatsApp', 'ALLE', 'phone call talk ring free', ARRAY[
    'chat-app|Open WhatsApp. The icon is green with a white telephone.',
    'person|Tap the person you want to call.',
    'hoerer-gruen|Tap the telephone at the top right.',
    'hoerer-rot|To hang up, tap the red telephone.']);

SELECT pg_temp.neu('Write a message with WhatsApp', 'WhatsApp', 'ALLE', 'text type send reply chat', ARRAY[
    'chat-app|Open WhatsApp.',
    'person|Tap the person you want to write to.',
    'tastatur|Tap the field “Message” at the bottom. Write your text.',
    'senden|Tap the arrow on the right. The message is now on its way.']);

SELECT pg_temp.neu('Take a photo', 'Photos', 'ALLE', 'camera picture shoot snap', ARRAY[
    'kamera|Open the “Camera”. The icon looks like a camera.',
    'bild|Hold the phone still. Point it at what you want to photograph.',
    'ausloeser|Tap the big round button at the bottom.',
    'bild-auswahl|The photo is done. You find it with your photos.']);

SELECT pg_temp.neu('Call someone', 'Phone', 'ALLE', 'phone call dial number contact ring', ARRAY[
    'hoerer-gruen|Open “Phone”. The icon is a telephone.',
    'kontakte|Tap “Contacts”. Then tap the name of the person.',
    'hoerer-gruen|Tap the telephone. The phone is now calling.',
    'hoerer-rot|To hang up, tap the red telephone.']);

SELECT pg_temp.neu('Write a text message', 'Phone', 'ALLE', 'sms message text send', ARRAY[
    'nachricht|Open “Messages”. The icon is a speech bubble.',
    'plus|Tap the sign for a new message. It is a pencil or a plus sign.',
    'person|Type in the name of the person. Then tap the suggestion.',
    'tastatur|Tap the field at the bottom and write your message.',
    'senden|Tap the arrow. The message is now on its way.']);

SELECT pg_temp.neu('Make the screen brighter', 'Settings', 'ANDROID', 'brightness dark light see display', ARRAY[
    'finger|Swipe down with your finger from the top edge of the screen.',
    'sonne|You see a slider with a small sun. Can you not see it? Then swipe down once more.',
    'regler|Move the slider to the right. The screen gets brighter.']);

SELECT pg_temp.neu('Make the screen brighter', 'Settings', 'IOS', 'brightness dark light see display', ARRAY[
    'finger|Swipe down from the top right corner. Does your iPhone have a round button at the bottom? Then swipe up from the bottom edge.',
    'sonne|You see a slider with a small sun.',
    'regler|Move the slider up. The screen gets brighter.']);

SELECT pg_temp.neu('Install an app', 'Apps', 'ANDROID', 'program download get play store new app', ARRAY[
    'einkaufstasche|Open the “Play Store”. The icon is a colourful triangle.',
    'lupe|Tap the search field at the top. Type in the name of the app.',
    'bild-auswahl|Tap the right app. Are you unsure? Then ask your family.',
    'herunterladen|Tap “Install”.',
    'haken|Wait until it says “Open”. The app is now on your phone.']);

SELECT pg_temp.neu('Install an app', 'Apps', 'IOS', 'program download get app store new app', ARRAY[
    'einkaufstasche|Open the “App Store”. The icon is blue with a white A.',
    'lupe|Tap “Search” at the bottom right. Type in the name of the app.',
    'bild-auswahl|Tap the right app. Are you unsure? Then ask your family.',
    'herunterladen|Tap “Get”. The iPhone may ask for your password or your face.',
    'haken|Wait until it says “Open”. The app is now on your iPhone.']);

SELECT pg_temp.neu('Switch on the torch', 'Settings', 'ANDROID', 'flashlight light lamp dark', ARRAY[
    'finger|Swipe down with your finger from the top edge of the screen.',
    'taschenlampe|Tap “Torch”. On some phones it is called “Flashlight”. The light on the back comes on.',
    'taschenlampe|To switch it off, tap it once more.']);

SELECT pg_temp.neu('Switch on the torch', 'Settings', 'IOS', 'flashlight light lamp dark', ARRAY[
    'finger|Swipe down from the top right corner. Does your iPhone have a round button at the bottom? Then swipe up from the bottom edge.',
    'taschenlampe|Tap the icon with the torch. The light on the back comes on.',
    'taschenlampe|To switch it off, tap it once more.']);

SELECT pg_temp.neu('Set an alarm', 'Settings', 'ALLE', 'clock wake up alarm time morning', ARRAY[
    'uhr-app|Open the app “Clock”.',
    'wecker|Tap “Alarm”.',
    'plus|Tap the plus sign.',
    'wecker|Set the time at which you want to be woken.',
    'haken|Tap “Save”.']);

SELECT pg_temp.neu('Block an unknown number', 'Phone', 'ANDROID', 'block caller spam nuisance annoying calls', ARRAY[
    'hoerer-gruen|Open “Phone”.',
    'ziffernblock|Tap “Recents”.',
    'finger|Keep your finger on the number that bothers you for a long moment.',
    'sperren|Tap “Block”. On some phones it is called “Block number”.']);

SELECT pg_temp.neu('Block an unknown number', 'Phone', 'IOS', 'block caller spam nuisance annoying calls', ARRAY[
    'hoerer-gruen|Open “Phone”.',
    'ziffernblock|Tap “Recents” at the bottom.',
    'person|Next to the number, tap the small “i” in a circle.',
    'sperren|Go all the way down. Tap “Block Caller”.']);

SELECT pg_temp.neu('Search for something on the internet', 'Internet', 'ANDROID', 'google search find browser chrome website', ARRAY[
    'weltkugel|Open “Chrome”. The icon is a colourful circle.',
    'lupe|Tap the field at the top.',
    'tastatur|Write what you are looking for. For example: weather tomorrow.',
    'haken|On the keyboard, tap the magnifying glass or the arrow at the bottom right.']);

SELECT pg_temp.neu('Search for something on the internet', 'Internet', 'IOS', 'google search find browser safari website', ARRAY[
    'weltkugel|Open “Safari”. The icon is a blue compass.',
    'lupe|Tap the field with the magnifying glass. It is at the bottom or at the top of the screen.',
    'tastatur|Write what you are looking for. For example: weather tomorrow.',
    'haken|On the keyboard, tap “go” at the bottom right.']);

SELECT pg_temp.neu('Restart the phone', 'Settings', 'ANDROID', 'switch off on frozen slow not responding reboot', ARRAY[
    'ein-aus|Hold down the power button on the side for a long moment. Nothing happens? Then hold down the volume down button as well.',
    'ein-aus|Tap “Restart”.',
    'haken|Wait until the phone is on again. That takes about one minute.']);

SELECT pg_temp.neu('Restart the phone', 'Settings', 'IOS', 'switch off on frozen slow not responding reboot', ARRAY[
    'seitentasten|Hold down the side button and one volume button. Does your iPhone have a round button at the bottom? Then hold down only the side button.',
    'regler|Move the slider “slide to power off” to the right.',
    'ein-aus|Wait half a minute. Then hold down the side button until an apple appears.']);

SELECT pg_temp.neu('Scan a QR code', 'Photos', 'ALLE', 'camera code square read link', ARRAY[
    'kamera|Open the “Camera”.',
    'qr|Hold the phone so that the whole QR code is in the picture.',
    'finger|A link appears on the screen. Tap it. Only scan codes that you trust.']);

SELECT pg_temp.neu('Call the emergency number 112', 'Phone', 'ALLE', 'emergency help ambulance fire brigade accident rescue', ARRAY[
    'hoerer-gruen|Open “Phone”.',
    'ziffernblock|Tap the keypad. Dial 1 1 2.',
    'notruf|Tap the green telephone. The emergency call is free. 112 works everywhere in the European Union.',
    'person|Say where you are and what has happened. Only hang up when you are told to.']);

SELECT pg_temp.neu('Switch off flight mode', 'Settings', 'ANDROID', 'airplane aeroplane mode no signal no network no internet phone not working', ARRAY[
    'flugzeug|Do you see a small aeroplane at the top of the screen? Then flight mode is on. Phone and internet do not work then.',
    'finger|Swipe down with your finger from the top edge of the screen.',
    'flugzeug|Tap the aeroplane. Flight mode is now off.']);

SELECT pg_temp.neu('Switch off flight mode', 'Settings', 'IOS', 'airplane aeroplane mode no signal no network no internet phone not working', ARRAY[
    'flugzeug|Do you see a small aeroplane at the top of the screen? Then flight mode is on. Phone and internet do not work then.',
    'finger|Swipe down from the top right corner. Does your iPhone have a round button at the bottom? Then swipe up from the bottom edge.',
    'flugzeug|Tap the orange aeroplane. It turns grey. Flight mode is now off.']);
