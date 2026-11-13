package de.digitalbegleiter.familie;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import de.digitalbegleiter.konto.GeraetInfo;

/** Die Formen, in denen Familien-Daten das Modul verlassen (DTOs). */
public final class FamilieDto {

    private FamilieDto() {
    }

    public record Kurz(UUID id, String name, Rolle rolle) {
    }

    public record Mitglied(UUID kontoId, String anzeigename, Rolle rolle, List<GeraetInfo> geraete) {
    }

    public record Details(UUID id, String name, Rolle meineRolle, List<Mitglied> mitglieder) {
    }

    /**
     * Eine frische Einladung. Der Code steht nur hier, in der Datenbank liegt nur sein Hash.
     *
     * @param qrSvg der QR-Code als SVG-Bild
     */
    public record NeueEinladung(String code, String link, Instant gueltigBis, String qrSvg) {
    }

    public record Anfrage(UUID id, UUID familieId, String vonName, HilfeThema thema, String themaText,
            String geteilterText, HilfeStatus status, Instant erstelltAm) {
    }
}
