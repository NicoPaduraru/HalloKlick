package de.digitalbegleiter.check.regel;

import java.util.List;

import de.digitalbegleiter.gemeinsam.Kategorie;
import de.digitalbegleiter.gemeinsam.Stufe;

/**
 * Ergebnis der Regel-Prüfung.
 *
 * @param unklar true, wenn die Regeln Warnzeichen sehen, aber nicht genug für Rot.
 *               Dann lohnt sich die Frage an die KI.
 */
public record RegelErgebnis(Stufe stufe, Kategorie kategorie, List<Merkmal> merkmale, boolean unklar) {
}
