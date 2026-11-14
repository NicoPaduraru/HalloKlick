package de.halloklick.check;

import java.util.List;
import java.util.UUID;

import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Stufe;

/**
 * Die Antwort an den Senior.
 *
 * @param geprueft false nur, wenn ein Foto gar nicht gelesen werden konnte
 * @param tipp bei Orange immer gesetzt: ein fester Rat, weil "keine Warnzeichen" nicht "sicher" heißt
 * @param zusatzHinweis gesetzt, wenn die KI nicht helfen konnte ("Bitte im Zweifel die Familie fragen")
 */
public record CheckErgebnis(
        UUID id,
        Stufe stufe,
        Kategorie kategorie,
        boolean geprueft,
        String titel,
        String erklaerung,
        List<String> merkmale,
        String naechsterSchritt,
        String hinweis,
        String zusatzHinweis,
        String tipp,
        boolean kiVerwendet) {
}
