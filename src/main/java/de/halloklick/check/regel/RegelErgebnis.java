package de.halloklick.check.regel;

import java.util.List;

import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Stufe;

/**
 * Ergebnis der Regel-Prüfung.
 *
 * @param unklar true, wenn die Regeln Warnzeichen sehen, aber nicht genug für Rot.
 *               Dann lohnt sich die Frage an die KI.
 */
public record RegelErgebnis(Stufe stufe, Kategorie kategorie, List<Merkmal> merkmale, boolean unklar) {
}
