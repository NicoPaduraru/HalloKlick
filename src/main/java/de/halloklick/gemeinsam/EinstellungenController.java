package de.halloklick.gemeinsam;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sagt der Web-App, welche Funktionen angeschaltet sind. Enthält keine Geheimnisse. */
@RestController
public class EinstellungenController {

    public record OeffentlicheEinstellungen(boolean sicherheitscheck, boolean anleitungen, boolean familie,
            boolean fotoPruefung, String pushSchluessel) {
    }

    private final Einstellungen einstellungen;

    public EinstellungenController(Einstellungen einstellungen) {
        this.einstellungen = einstellungen;
    }

    @GetMapping("/api/einstellungen")
    OeffentlicheEinstellungen einstellungen() {
        var funktionen = einstellungen.funktionen();
        String push = einstellungen.push().eingerichtet() ? einstellungen.push().oeffentlicherSchluessel() : null;
        return new OeffentlicheEinstellungen(funktionen.sicherheitscheck(), funktionen.anleitungen(),
                funktionen.familie(), einstellungen.ki().einsatzbereit(), push);
    }
}
