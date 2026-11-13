package de.digitalbegleiter.ki;

/**
 * Der nächste Schritt für den Senior. Die KI darf nur aus dieser festen Liste wählen.
 * Den Text dazu schreiben wir selbst. So kann eine manipulierte Nachricht die KI nicht
 * dazu bringen, eine Telefonnummer oder einen Link als "nächsten Schritt" auszugeben.
 */
public enum NaechsterSchritt {
    NICHT_REAGIEREN,
    BANK_UEBER_BEKANNTE_NUMMER_ANRUFEN,
    ABSENDER_UEBER_BEKANNTE_NUMMER_ANRUFEN,
    NUR_IN_OFFIZIELLER_APP_NACHSEHEN,
    FAMILIE_FRAGEN
}
