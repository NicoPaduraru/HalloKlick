package de.halloklick.ki;

/** Die KI konnte nicht antworten. Der Check läuft dann nur mit den Regeln weiter. */
public class KiNichtVerfuegbarException extends RuntimeException {

    private final KiErgebnis.Grund grund;

    public KiNichtVerfuegbarException(KiErgebnis.Grund grund, String nachricht) {
        super(nachricht);
        this.grund = grund;
    }

    public KiErgebnis.Grund grund() {
        return grund;
    }
}
