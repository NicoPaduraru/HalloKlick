package de.digitalbegleiter.gemeinsam;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Alle eigenen Einstellungen aus application.yml (Abschnitt "digitalbegleiter"). */
@ConfigurationProperties("digitalbegleiter")
public record Einstellungen(
        String basisUrl,
        String internToken,
        Cookies cookies,
        Funktionen funktionen,
        Ki ki,
        Push push,
        Mail mail,
        Begrenzung begrenzung) {

    public record Cookies(boolean secure) {
    }

    /** Feature-Flags: Funktionen einzeln an- und abschaltbar. */
    public record Funktionen(boolean sicherheitscheck, boolean anleitungen, boolean familie) {
    }

    public record Ki(
            boolean aktiv,
            String apiKey,
            String modell,
            int tageslimit,
            BigDecimal monatsbudgetUsd,
            BigDecimal preisEingabeProMio,
            BigDecimal preisAusgabeProMio) {

        /** Die KI läuft nur mit Schalter, Schlüssel und einem Budget über 0. */
        public boolean einsatzbereit() {
            return aktiv
                    && apiKey != null && !apiKey.isBlank()
                    && monatsbudgetUsd != null && monatsbudgetUsd.signum() > 0;
        }
    }

    public record Push(String oeffentlicherSchluessel, String privaterSchluessel, String betreff) {

        public boolean eingerichtet() {
            return oeffentlicherSchluessel != null && !oeffentlicherSchluessel.isBlank()
                    && privaterSchluessel != null && !privaterSchluessel.isBlank();
        }
    }

    public record Mail(String absender) {

        public boolean eingerichtet() {
            return absender != null && !absender.isBlank();
        }
    }

    public record Begrenzung(int proMinute, int heikelProMinute) {
    }
}
