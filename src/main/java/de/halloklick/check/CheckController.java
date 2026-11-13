package de.halloklick.check;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.halloklick.gemeinsam.ApiFehler;
import de.halloklick.gemeinsam.Einstellungen;
import de.halloklick.gemeinsam.sicherheit.Angemeldet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
class CheckController {

    record FeedbackAnfrage(@NotNull(message = "Bitte wählen Sie Ja oder Nein.") Boolean hilfreich) {
    }

    private final CheckService checks;
    private final Einstellungen einstellungen;

    CheckController(CheckService checks, Einstellungen einstellungen) {
        this.checks = checks;
        this.einstellungen = einstellungen;
    }

    /** Text ODER Foto prüfen. Genau eines von beiden muss da sein. */
    @PostMapping(path = "/api/checks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    CheckErgebnis pruefe(@RequestParam(required = false) String text,
            @RequestParam(required = false) MultipartFile bild, @AuthenticationPrincipal Angemeldet ich)
            throws IOException {
        if (!einstellungen.funktionen().sicherheitscheck()) {
            throw ApiFehler.abgeschaltet();
        }
        boolean hatText = text != null && !text.isBlank();
        boolean hatBild = bild != null && !bild.isEmpty();
        if (hatText == hatBild) {
            throw ApiFehler.ungueltig("Bitte fügen Sie entweder einen Text ein oder wählen Sie ein Foto.");
        }
        if (hatBild) {
            if (bild.getSize() > BildPruefung.HOECHSTE_GROESSE_BYTES) {
                throw new ApiFehler(HttpStatus.PAYLOAD_TOO_LARGE,
                        "Das Foto ist zu groß. Es darf höchstens 5 MB haben.");
            }
            return checks.pruefeBild(ich.kontoId(), bild.getBytes());
        }
        return checks.pruefeText(ich.kontoId(), text);
    }

    @PostMapping("/api/checks/{id}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void feedback(@PathVariable UUID id, @Valid @RequestBody FeedbackAnfrage anfrage,
            @AuthenticationPrincipal Angemeldet ich) {
        checks.gibFeedback(ich.kontoId(), id, anfrage.hilfreich());
    }
}
