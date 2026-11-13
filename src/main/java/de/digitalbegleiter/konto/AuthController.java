package de.digitalbegleiter.konto;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.digitalbegleiter.gemeinsam.sicherheit.Angemeldet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registrieren, Anmelden und Abmelden für Angehörige. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    record RegistrierAnfrage(
            @NotBlank(message = "Bitte geben Sie Ihre E-Mail-Adresse ein.")
            @Email(message = "Diese E-Mail-Adresse ist nicht gültig.")
            @Size(max = 254, message = "Diese E-Mail-Adresse ist zu lang.")
            String email,
            @NotBlank(message = "Bitte wählen Sie ein Passwort.")
            @Size(min = 10, max = 72, message = "Das Passwort muss zwischen 10 und 72 Zeichen lang sein.")
            String passwort,
            @NotBlank(message = "Bitte geben Sie Ihren Namen ein.")
            @Size(max = 80, message = "Der Name ist zu lang.")
            String anzeigename) {
    }

    record AnmeldeAnfrage(
            @NotBlank(message = "Bitte geben Sie Ihre E-Mail-Adresse ein.") @Size(max = 254) String email,
            @NotBlank(message = "Bitte geben Sie Ihr Passwort ein.") @Size(max = 72) String passwort) {
    }

    private final KontoService konten;
    private final SecurityContextRepository kontextSpeicher;

    AuthController(KontoService konten, SecurityContextRepository kontextSpeicher) {
        this.konten = konten;
        this.kontextSpeicher = kontextSpeicher;
    }

    @PostMapping("/registrieren")
    @ResponseStatus(HttpStatus.CREATED)
    KontoInfo registrieren(@Valid @RequestBody RegistrierAnfrage anfrage, HttpServletRequest http,
            HttpServletResponse antwort) {
        KontoInfo konto = konten.registriereAngehoerigen(anfrage.email(), anfrage.passwort(), anfrage.anzeigename());
        meldeAn(konto, http, antwort);
        return konto;
    }

    @PostMapping("/anmelden")
    KontoInfo anmelden(@Valid @RequestBody AnmeldeAnfrage anfrage, HttpServletRequest http,
            HttpServletResponse antwort) {
        KontoInfo konto = konten.pruefeAnmeldung(anfrage.email(), anfrage.passwort());
        meldeAn(konto, http, antwort);
        return konto;
    }

    @PostMapping("/abmelden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void abmelden(HttpServletRequest http) {
        HttpSession sitzung = http.getSession(false);
        if (sitzung != null) {
            sitzung.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private void meldeAn(KontoInfo konto, HttpServletRequest http, HttpServletResponse antwort) {
        // Neue Sitzungs-Nummer nach dem Anmelden (Schutz vor Session Fixation)
        if (http.getSession(false) != null) {
            http.changeSessionId();
        }
        SecurityContext kontext = SecurityContextHolder.createEmptyContext();
        kontext.setAuthentication(new Angemeldet(konto.id(), Angemeldet.ANGEHOERIGER, null).alsAuthentication());
        SecurityContextHolder.setContext(kontext);
        kontextSpeicher.saveContext(kontext, http, antwort);
    }
}
