package de.halloklick.familie;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.halloklick.gemeinsam.sicherheit.Angemeldet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
class FamilienController {

    record FamilieAnfrage(
            @NotBlank(message = "Bitte geben Sie der Familie einen Namen.")
            @Size(max = 80, message = "Der Name ist zu lang.")
            String name) {
    }

    record EinladungAnfrage(@Size(max = 80, message = "Der Name ist zu lang.") String nameFuerSenior) {
    }

    record KoppelAnfrage(
            @NotBlank(message = "Bitte geben Sie den Code ein.")
            @Size(max = 40, message = "Der Code ist zu lang.")
            String code) {
    }

    record HilfeAnfrage(
            @NotNull(message = "Bitte wählen Sie ein Thema.") HilfeThema thema,
            @Size(max = 2000, message = "Der Text ist zu lang.") String geteilterText) {
    }

    record HilfeAntwort(int familien) {
    }

    record StatusAnfrage(@NotNull(message = "Der Status fehlt.") HilfeStatus status) {
    }

    private final FamilienService familien;
    private final HilfeanfrageService hilfeanfragen;

    FamilienController(FamilienService familien, HilfeanfrageService hilfeanfragen) {
        this.familien = familien;
        this.hilfeanfragen = hilfeanfragen;
    }

    // ---------- Für Angehörige ----------

    @PostMapping("/api/familien")
    @ResponseStatus(HttpStatus.CREATED)
    FamilieDto.Kurz erstelle(@Valid @RequestBody FamilieAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        return familien.erstelle(ich.kontoId(), anfrage.name());
    }

    @GetMapping("/api/familien")
    List<FamilieDto.Kurz> meineFamilien(@AuthenticationPrincipal Angemeldet ich) {
        return familien.familienVon(ich.kontoId());
    }

    @GetMapping("/api/familien/{id}")
    FamilieDto.Details details(@PathVariable UUID id, @AuthenticationPrincipal Angemeldet ich) {
        return familien.details(id, ich.kontoId());
    }

    @PostMapping("/api/familien/{id}/einladungen")
    @ResponseStatus(HttpStatus.CREATED)
    FamilieDto.NeueEinladung einladung(@PathVariable UUID id,
            @Valid @RequestBody(required = false) EinladungAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        return familien.erstelleEinladung(id, ich.kontoId(), anfrage == null ? null : anfrage.nameFuerSenior());
    }

    @GetMapping("/api/familien/{id}/hilfeanfragen")
    List<FamilieDto.Anfrage> anfragen(@PathVariable UUID id, @RequestParam(required = false) HilfeStatus status,
            @AuthenticationPrincipal Angemeldet ich) {
        return hilfeanfragen.anfragenDerFamilie(id, ich.kontoId(), status);
    }

    @PatchMapping("/api/hilfeanfragen/{id}")
    FamilieDto.Anfrage setzeStatus(@PathVariable UUID id, @Valid @RequestBody StatusAnfrage anfrage,
            @AuthenticationPrincipal Angemeldet ich) {
        return hilfeanfragen.setzeStatus(id, ich.kontoId(), anfrage.status());
    }

    /** Verlorenes Handy sperren. */
    @DeleteMapping("/api/geraete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void sperreGeraet(@PathVariable UUID id, @AuthenticationPrincipal Angemeldet ich) {
        familien.sperreGeraet(ich.kontoId(), id);
    }

    // ---------- Für Senioren ----------

    /** Einladungscode einlösen: Das Gerät gehört danach zur Familie. */
    @PostMapping("/api/geraete/koppeln")
    FamilieDto.Kurz koppeln(@Valid @RequestBody KoppelAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        return familien.koppeln(ich.kontoId(), anfrage.code());
    }

    @PostMapping("/api/hilfeanfragen")
    @ResponseStatus(HttpStatus.CREATED)
    HilfeAntwort hilfe(@Valid @RequestBody HilfeAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        return new HilfeAntwort(hilfeanfragen.erstelle(ich.kontoId(), anfrage.thema(), anfrage.geteilterText()));
    }

    // ---------- Für alle Angemeldeten ----------

    /** Mit welchen Familien bin ich verbunden? Die Web-App braucht das für "Familie fragen". */
    @GetMapping("/api/ich/familien")
    List<FamilieDto.Kurz> familienVonMir(@AuthenticationPrincipal Angemeldet ich) {
        return familien.familienVon(ich.kontoId());
    }
}
