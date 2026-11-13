package de.digitalbegleiter.ki;

import java.util.Optional;

/** Entweder eine Einschätzung der KI oder der Grund, warum es keine gibt. */
public record KiErgebnis(KiEinschaetzung einschaetzung, Grund grund) {

    public enum Grund {
        OK,
        /** KI ist nicht angeschaltet oder nicht eingerichtet. */
        ABGESCHALTET,
        /** Diese Person hat heute schon alle KI-Checks verbraucht. */
        TAGESLIMIT,
        /** Das Monatsbudget für alle ist erreicht. */
        MONATSBUDGET,
        /** Die KI hat die Antwort verweigert. */
        ABGELEHNT,
        /** Ausfall, Zeitüberschreitung oder unbrauchbare Antwort. */
        FEHLER
    }

    static KiErgebnis ok(KiEinschaetzung einschaetzung) {
        return new KiErgebnis(einschaetzung, Grund.OK);
    }

    static KiErgebnis ohne(Grund grund) {
        return new KiErgebnis(null, grund);
    }

    public Optional<KiEinschaetzung> wennVorhanden() {
        return Optional.ofNullable(einschaetzung);
    }
}
