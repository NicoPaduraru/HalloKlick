package de.digitalbegleiter.ki;

import java.util.List;

import de.digitalbegleiter.gemeinsam.Kategorie;
import de.digitalbegleiter.gemeinsam.Stufe;

/**
 * Die Antwort der KI in festem Format.
 *
 * @param erkannterText bei Fotos: der Text, den die KI auf dem Bild gelesen hat (sonst leer).
 *                      Darauf läuft danach noch einmal die Regel-Prüfung.
 */
public record KiEinschaetzung(
        Stufe stufe,
        Kategorie kategorie,
        List<String> merkmale,
        String erklaerung,
        NaechsterSchritt naechsterSchritt,
        String erkannterText,
        long eingabeTokens,
        long ausgabeTokens) {
}
