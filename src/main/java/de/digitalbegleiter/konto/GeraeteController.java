package de.digitalbegleiter.konto;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.digitalbegleiter.gemeinsam.sicherheit.Angemeldet;
import de.digitalbegleiter.gemeinsam.sicherheit.GeraeteCookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
class GeraeteController {

    record NeuesGeraetAnfrage(Plattform plattform) {
    }

    record GeraetAntwort(UUID kontoId, UUID geraetId) {
    }

    private final GeraeteService geraete;
    private final GeraeteCookie cookie;

    GeraeteController(GeraeteService geraete, GeraeteCookie cookie) {
        this.geraete = geraete;
        this.cookie = cookie;
    }

    /**
     * Legt ein neues, anonymes Gerät an. Der Schlüssel kommt nur als HttpOnly-Cookie
     * zurück, nie im Antworttext.
     */
    @PostMapping("/api/geraete")
    @ResponseStatus(HttpStatus.CREATED)
    GeraetAntwort neuesGeraet(@RequestBody(required = false) NeuesGeraetAnfrage anfrage,
            @AuthenticationPrincipal Angemeldet ich, HttpServletResponse antwort) {
        if (ich != null && ich.istSenior()) {
            // Dieses Handy hat schon ein Gerät: nichts Neues anlegen
            return new GeraetAntwort(ich.kontoId(), ich.geraetId());
        }
        Plattform plattform = anfrage == null || anfrage.plattform() == null ? Plattform.SONSTIGE : anfrage.plattform();
        GeraeteService.NeuesGeraet neu = geraete.erstelleAnonymesGeraet(plattform);
        cookie.setze(antwort, neu.schluessel());
        return new GeraetAntwort(neu.kontoId(), neu.geraetId());
    }
}
