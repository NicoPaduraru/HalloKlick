package de.digitalbegleiter.ki;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.digitalbegleiter.gemeinsam.Einstellungen;

@Configuration
class KiKonfiguration {

    /**
     * Wählt die Umsetzung von {@link NachrichtenPruefer}. Ohne Schlüssel oder Budget gibt es
     * einen Prüfer, der immer "nicht verfügbar" meldet. Dann läuft nur die Regel-Prüfung.
     */
    @Bean
    NachrichtenPruefer nachrichtenPruefer(Einstellungen einstellungen) {
        Einstellungen.Ki ki = einstellungen.ki();
        if (ki.einsatzbereit()) {
            return new ClaudeNachrichtenPruefer(ki.apiKey(), ki.modell());
        }
        return eingabe -> {
            throw new KiNichtVerfuegbarException(KiErgebnis.Grund.ABGESCHALTET, "KI ist abgeschaltet");
        };
    }
}
