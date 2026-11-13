package de.halloklick.gemeinsam;

import java.util.UUID;

/**
 * Jedes Modul, das Daten zu einer Person speichert, liefert hier seinen Teil
 * für die Datenauskunft (DSGVO, Art. 15).
 */
public interface DatenauskunftBeitrag {

    /** Überschrift des Abschnitts, zum Beispiel "sicherheitschecks". */
    String name();

    /** Die gespeicherten Daten zu diesem Konto, als einfache Listen und Maps. */
    Object datenVon(UUID kontoId);
}
