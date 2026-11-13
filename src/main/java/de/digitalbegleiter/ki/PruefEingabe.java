package de.digitalbegleiter.ki;

/**
 * Was an die KI geht: NUR der Prüftext oder das Bild. Nie Name, Konto oder Gerät.
 * Genau eines von beiden ist gesetzt. Das Bild ist immer ein neu kodiertes JPEG ohne Metadaten.
 */
public record PruefEingabe(String text, byte[] jpegBild) {

    public static PruefEingabe ausText(String text) {
        return new PruefEingabe(text, null);
    }

    public static PruefEingabe ausBild(byte[] jpegBild) {
        return new PruefEingabe(null, jpegBild);
    }

    public boolean istBild() {
        return jpegBild != null;
    }
}
