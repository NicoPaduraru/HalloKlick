package de.digitalbegleiter.benachrichtigung;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.digitalbegleiter.gemeinsam.sicherheit.Angemeldet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Angehörige schalten hier Benachrichtigungen für ihren Browser an oder aus. */
@RestController
@RequestMapping("/api/push-abos")
class PushController {

    record AboAnfrage(
            @NotBlank(message = "Die Adresse fehlt.") @Size(max = 1000, message = "Die Adresse ist zu lang.")
            String endpoint,
            @NotBlank(message = "Der Schlüssel fehlt.") @Size(max = 200, message = "Der Schlüssel ist zu lang.")
            String p256dh,
            @NotBlank(message = "Der Schlüssel fehlt.") @Size(max = 100, message = "Der Schlüssel ist zu lang.")
            String auth) {
    }

    record AbmeldeAnfrage(@NotBlank(message = "Die Adresse fehlt.") @Size(max = 1000) String endpoint) {
    }

    private final BenachrichtigungsService benachrichtigung;

    PushController(BenachrichtigungsService benachrichtigung) {
        this.benachrichtigung = benachrichtigung;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void anmelden(@Valid @RequestBody AboAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        benachrichtigung.speichereAbo(ich.kontoId(), anfrage.endpoint(), anfrage.p256dh(), anfrage.auth());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void abmelden(@Valid @RequestBody AbmeldeAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        benachrichtigung.entferneAbo(ich.kontoId(), anfrage.endpoint());
    }
}
