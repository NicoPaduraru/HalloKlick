package de.digitalbegleiter.anleitung;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.digitalbegleiter.gemeinsam.ApiFehler;
import de.digitalbegleiter.gemeinsam.Einstellungen;
import de.digitalbegleiter.gemeinsam.sicherheit.Angemeldet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
class AnleitungController {

    record AnleitungAnfrage(
            @NotBlank(message = "Bitte geben Sie einen Titel ein.")
            @Size(max = 120, message = "Der Titel ist zu lang.")
            String titel,
            @Size(max = 40, message = "Die Kategorie ist zu lang.")
            String kategorie,
            @NotNull(message = "Bitte wählen Sie ein Handy aus.")
            AnleitungPlattform plattform,
            @NotEmpty(message = "Bitte schreiben Sie mindestens einen Schritt.")
            @Size(max = 20, message = "Eine Anleitung kann höchstens 20 Schritte haben.")
            List<@NotBlank(message = "Ein Schritt ist leer.")
                 @Size(max = 600, message = "Ein Schritt ist zu lang.") String> schritte) {

        String kategorieOderStandard() {
            return kategorie == null || kategorie.isBlank() ? "Familie" : kategorie;
        }
    }

    private final AnleitungService anleitungen;
    private final Einstellungen einstellungen;

    AnleitungController(AnleitungService anleitungen, Einstellungen einstellungen) {
        this.anleitungen = anleitungen;
        this.einstellungen = einstellungen;
    }

    // --- Für alle ---

    @GetMapping("/api/anleitungen")
    List<AnleitungDto.Kurz> suche(@RequestParam(required = false) String suche,
            @RequestParam(required = false) AnleitungPlattform plattform, @AuthenticationPrincipal Angemeldet ich) {
        pruefeAngeschaltet();
        return anleitungen.suche(suche, plattform, ich == null ? null : ich.kontoId());
    }

    @GetMapping("/api/anleitungen/{id}")
    AnleitungDto.Voll anleitung(@PathVariable UUID id, @AuthenticationPrincipal Angemeldet ich) {
        pruefeAngeschaltet();
        return anleitungen.lade(id, ich == null ? null : ich.kontoId());
    }

    // --- Eigene Anleitungen einer Familie (nur Angehörige, nur Mitglieder) ---

    @GetMapping("/api/familien/{familieId}/anleitungen")
    List<AnleitungDto.Voll> derFamilie(@PathVariable UUID familieId, @AuthenticationPrincipal Angemeldet ich) {
        return anleitungen.anleitungenDerFamilie(familieId, ich.kontoId());
    }

    @PostMapping("/api/familien/{familieId}/anleitungen")
    @ResponseStatus(HttpStatus.CREATED)
    AnleitungDto.Voll erstelle(@PathVariable UUID familieId, @Valid @RequestBody AnleitungAnfrage anfrage,
            @AuthenticationPrincipal Angemeldet ich) {
        return anleitungen.erstelle(familieId, ich.kontoId(), anfrage.titel(), anfrage.kategorieOderStandard(),
                anfrage.plattform(), anfrage.schritte());
    }

    @PutMapping("/api/familien/{familieId}/anleitungen/{anleitungId}")
    AnleitungDto.Voll aendere(@PathVariable UUID familieId, @PathVariable UUID anleitungId,
            @Valid @RequestBody AnleitungAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        return anleitungen.aendere(familieId, anleitungId, ich.kontoId(), anfrage.titel(),
                anfrage.kategorieOderStandard(), anfrage.plattform(), anfrage.schritte());
    }

    @DeleteMapping("/api/familien/{familieId}/anleitungen/{anleitungId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loesche(@PathVariable UUID familieId, @PathVariable UUID anleitungId,
            @AuthenticationPrincipal Angemeldet ich) {
        anleitungen.loesche(familieId, anleitungId, ich.kontoId());
    }

    private void pruefeAngeschaltet() {
        if (!einstellungen.funktionen().anleitungen()) {
            throw ApiFehler.abgeschaltet();
        }
    }
}
