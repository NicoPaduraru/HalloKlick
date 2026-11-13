package de.digitalbegleiter.gemeinsam;

/**
 * Warnstufe eines Sicherheitschecks. Es gibt absichtlich kein Grün:
 * Wir können nie versprechen, dass eine Nachricht sicher ist.
 */
public enum Stufe {
    /** Keine typischen Warnzeichen gefunden. */
    ORANGE,
    /** Sieht nach Betrug aus. */
    ROT;

    /** Die höhere Warnstufe gewinnt immer. */
    public static Stufe hoechste(Stufe a, Stufe b) {
        return (a == ROT || b == ROT) ? ROT : ORANGE;
    }
}
