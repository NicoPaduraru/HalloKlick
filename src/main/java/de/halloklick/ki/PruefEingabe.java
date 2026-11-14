package de.halloklick.ki;

/**
 * Was an die KI geht: NUR der Prüftext oder das Bild, dazu die Sprache für die Antwort.
 * Nie Name, Konto oder Gerät. Genau eines von Text und Bild ist gesetzt.
 * Das Bild ist immer ein neu kodiertes JPEG ohne Metadaten.
 *
 * @param antwortSprache in welcher Sprache die KI ihre Erklärung schreiben soll, zum Beispiel "Deutsch"
 */
public record PruefEingabe(String text, byte[] jpegBild, String antwortSprache) {

    private static final String STANDARD_SPRACHE = "Deutsch";

    public PruefEingabe {
        if (antwortSprache == null || antwortSprache.isBlank()) {
            antwortSprache = STANDARD_SPRACHE;
        }
    }

    public static PruefEingabe ausText(String text) {
        return new PruefEingabe(text, null, STANDARD_SPRACHE);
    }

    public static PruefEingabe ausText(String text, String antwortSprache) {
        return new PruefEingabe(text, null, antwortSprache);
    }

    public static PruefEingabe ausBild(byte[] jpegBild) {
        return new PruefEingabe(null, jpegBild, STANDARD_SPRACHE);
    }

    public static PruefEingabe ausBild(byte[] jpegBild, String antwortSprache) {
        return new PruefEingabe(null, jpegBild, antwortSprache);
    }

    public boolean istBild() {
        return jpegBild != null;
    }
}
