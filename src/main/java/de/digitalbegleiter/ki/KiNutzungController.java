package de.digitalbegleiter.ki;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import de.digitalbegleiter.gemeinsam.ApiFehler;
import de.digitalbegleiter.gemeinsam.Einstellungen;
import de.digitalbegleiter.gemeinsam.Schluessel;

/**
 * Interne Übersicht für den Betreiber: KI-Aufrufe und geschätzte Kosten pro Tag.
 * Geschützt durch einen geheimen Schlüssel (INTERN_TOKEN). Ohne Schlüssel gibt es die Seite nicht.
 *
 * <pre>curl -H "X-Intern-Token: ..." https://.../api/intern/ki-nutzung</pre>
 */
@RestController
class KiNutzungController {

    private final KiDienst ki;
    private final String internToken;

    KiNutzungController(KiDienst ki, Einstellungen einstellungen) {
        this.ki = ki;
        this.internToken = einstellungen.internToken();
    }

    @GetMapping("/api/intern/ki-nutzung")
    KiDienst.Uebersicht uebersicht(@RequestHeader(name = "X-Intern-Token", required = false) String token) {
        boolean eingerichtet = internToken != null && internToken.length() >= 20;
        if (!eingerichtet || !Schluessel.gleich(internToken, token)) {
            throw ApiFehler.nichtGefunden("Diese Seite gibt es nicht.");
        }
        return ki.uebersicht();
    }
}
