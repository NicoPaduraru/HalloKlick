package de.halloklick.check;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Teilen an HalloKlick" (Web Share Target): In WhatsApp oder SMS lange auf eine Nachricht tippen,
 * "Teilen" wählen, dann "HalloKlick". Das Handy schickt die Nachricht dann an die Adresse /teilen.
 *
 * <p>Normalerweise kommt diese Anfrage gar nicht beim Server an: Der Service Worker im Handy
 * (sw.js) fängt sie ab und reicht den Text direkt an die App weiter. Dieser Controller ist nur
 * das Sicherheitsnetz, falls der Service Worker einmal nicht läuft. Er liest den Inhalt nicht
 * und speichert nichts. Er schickt den Nutzer nur zur Seite "Ist das echt?".
 */
@RestController
class TeilenController {

    @PostMapping("/teilen")
    ResponseEntity<Void> teilen() {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/#/pruefen")).build();
    }
}
