package de.halloklick.konto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.halloklick.gemeinsam.ApiFehler;
import de.halloklick.gemeinsam.DatenauskunftBeitrag;
import de.halloklick.gemeinsam.Sprache;
import de.halloklick.gemeinsam.sicherheit.Angemeldet;
import de.halloklick.gemeinsam.sicherheit.GeraeteCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Das eigene Konto: wer bin ich, Datenauskunft und Konto löschen (DSGVO). */
@RestController
@RequestMapping("/api/ich")
class IchController {

    private final KontoService konten;
    private final GeraeteService geraete;
    private final List<DatenauskunftBeitrag> beitraege;
    private final GeraeteCookie cookie;

    IchController(KontoService konten, GeraeteService geraete, List<DatenauskunftBeitrag> beitraege,
            GeraeteCookie cookie) {
        this.konten = konten;
        this.geraete = geraete;
        this.beitraege = beitraege;
        this.cookie = cookie;
    }

    @GetMapping
    KontoInfo ich(@AuthenticationPrincipal Angemeldet ich) {
        return konten.finde(ich.kontoId())
                .orElseThrow(() -> new ApiFehler(HttpStatus.UNAUTHORIZED, "Bitte melden Sie sich an."));
    }

    record SprachAnfrage(String sprache) {
    }

    /**
     * Die Web-App meldet, welche Sprache der Nutzer gewählt hat. Wir merken sie uns am Konto,
     * damit auch Benachrichtigungen in dieser Sprache ankommen.
     */
    @PutMapping("/sprache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setzeSprache(@RequestBody SprachAnfrage anfrage, @AuthenticationPrincipal Angemeldet ich) {
        konten.setzeSprache(ich.kontoId(), Sprache.von(anfrage.sprache()));
    }

    /** Datenauskunft: alles, was wir zu diesem Konto gespeichert haben. */
    @GetMapping("/daten")
    Map<String, Object> daten(@AuthenticationPrincipal Angemeldet ich) {
        Map<String, Object> daten = new LinkedHashMap<>();
        daten.put("konto", ich(ich));
        daten.put("geraete", geraete.geraeteVon(ich.kontoId()));
        for (DatenauskunftBeitrag beitrag : beitraege) {
            daten.put(beitrag.name(), beitrag.datenVon(ich.kontoId()));
        }
        return daten;
    }

    /** Löscht das eigene Konto mit allen Daten. Das lässt sich nicht rückgängig machen. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void loeschen(@AuthenticationPrincipal Angemeldet ich, HttpServletRequest http, HttpServletResponse antwort) {
        konten.loesche(ich.kontoId());
        HttpSession sitzung = http.getSession(false);
        if (sitzung != null) {
            sitzung.invalidate();
        }
        cookie.entferne(antwort);
        SecurityContextHolder.clearContext();
    }
}
