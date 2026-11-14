package de.halloklick.check.regel;

/** Ein Warnzeichen, das die Regel-Prüfung gefunden hat, mit Erklärung in einfacher Sprache. */
public enum Merkmal {
    NACHGEMACHTE_ADRESSE("Die Internetadresse sieht aus wie von einer bekannten Firma. Sie ist aber nicht die echte."),
    IP_LINK("Der Link führt zu einer Adresse aus Zahlen. Seriöse Firmen machen das nicht."),
    KURZLINK("Die Nachricht enthält einen gekürzten Link. Man sieht nicht, wohin er führt."),
    UNGEWOEHNLICHE_ADRESSE("Der Link führt zu einer ungewöhnlichen Internetadresse."),
    LINK("Die Nachricht enthält einen Link."),
    ZEITDRUCK("Die Nachricht macht Zeitdruck. Sie sollen schnell handeln."),
    DROHUNG("Die Nachricht droht, zum Beispiel mit Sperrung, Mahnung oder Strafe."),
    GEHEIME_DATEN("Sie sollen geheime Daten nennen, zum Beispiel PIN, TAN oder Passwort. "
            + "Das verlangt keine echte Bank und keine echte Firma."),
    DATENABFRAGE("Sie sollen Ihre Daten bestätigen, eingeben oder sich anmelden."),
    GELDFORDERUNG("Sie sollen Geld zahlen oder überweisen."),
    UNGEWOEHNLICHE_ZAHLART("Sie sollen mit Gutscheinkarten, Bitcoin oder Ähnlichem bezahlen. Das ist typisch für Betrug."),
    NEUE_NUMMER("Jemand schreibt als Familienmitglied von einer neuen Nummer. Das ist ein bekannter Trick."),
    GEWINN("Ihnen wird ein Gewinn, ein Geschenk oder eine Erbschaft versprochen."),
    FERNZUGRIFF("Sie sollen ein Programm installieren oder jemandem Zugriff auf Ihr Gerät geben."),
    SCHNELLES_GELD("Ihnen wird schnelles Geld versprochen, zum Beispiel ein Nebenjob oder eine Geldanlage."),
    FALSCHE_POLIZEI("Jemand gibt sich als Polizei, Staatsanwalt oder Bank aus und will Bargeld oder Wertsachen. "
            + "Die echte Polizei holt niemals Geld oder Schmuck ab."),
    SCHOCK("Die Nachricht macht Angst, zum Beispiel mit einem Unfall oder einer Festnahme, und verlangt Geld."),
    ERPRESSUNG("Jemand behauptet, Sie beobachtet zu haben, und verlangt Geld."),
    RUECKRUF("Sie sollen eine Telefonnummer aus der Nachricht anrufen.");

    private final String erklaerung;

    Merkmal(String erklaerung) {
        this.erklaerung = erklaerung;
    }

    public String erklaerung() {
        return erklaerung;
    }
}
