package de.digitalbegleiter.anleitung;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Die Formen, in denen Anleitungen das Modul verlassen (DTOs). */
final class AnleitungDto {

    private AnleitungDto() {
    }

    /** Für Suchergebnisse: ohne Schritte. */
    record Kurz(UUID id, String titel, String kategorie, AnleitungPlattform plattform, AnleitungQuelle quelle) {

        static Kurz von(Anleitung anleitung) {
            return new Kurz(anleitung.getId(), anleitung.getTitel(), anleitung.getKategorie(),
                    anleitung.getPlattform(), anleitung.getQuelle());
        }
    }

    record Schritt(int nummer, String text, String bildPfad) {
    }

    record Voll(UUID id, String titel, String kategorie, AnleitungPlattform plattform, AnleitungQuelle quelle,
            UUID familieId, LocalDate geprueftAm, List<Schritt> schritte) {

        static Voll von(Anleitung anleitung) {
            List<Schritt> schritte = anleitung.getSchritte().stream()
                    .map(s -> new Schritt(s.getNummer(), s.getText(), s.getBildPfad()))
                    .toList();
            return new Voll(anleitung.getId(), anleitung.getTitel(), anleitung.getKategorie(),
                    anleitung.getPlattform(), anleitung.getQuelle(), anleitung.getFamilieId(),
                    anleitung.getGeprueftAm(), schritte);
        }
    }
}
