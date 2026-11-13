package de.digitalbegleiter.ki;

/**
 * Schnittstelle zum KI-Anbieter. Die erste Umsetzung ist {@link ClaudeNachrichtenPruefer}.
 * Ein anderer Anbieter wäre eine zweite Klasse, der Rest des Codes ändert sich nicht.
 */
public interface NachrichtenPruefer {

    /**
     * Schätzt eine Nachricht ein.
     *
     * @throws KiNichtVerfuegbarException wenn die KI ausfällt, ablehnt oder abgeschaltet ist
     */
    KiEinschaetzung pruefe(PruefEingabe eingabe);
}
