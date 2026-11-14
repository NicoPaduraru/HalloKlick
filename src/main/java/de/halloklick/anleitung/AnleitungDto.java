package de.halloklick.anleitung;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Die Formen, in denen Anleitungen das Modul verlassen (DTOs). */
final class AnleitungDto {

    private AnleitungDto() {
    }

    /** Für Suchergebnisse: ohne Schritte. */
    record Kurz(UUID id, String titel, String kategorie, AnleitungPlattform plattform, AnleitungQuelle quelle,
            String bildPfad) {

        static Kurz von(Anleitung anleitung) {
            // Das Bild des ersten Schritts dient in der Liste als kleines Erkennungszeichen
            String bild = anleitung.getSchritte().isEmpty() ? null : anleitung.getSchritte().get(0).getBildPfad();
            return new Kurz(anleitung.getId(), anleitung.getTitel(), anleitung.getKategorie(),
                    anleitung.getPlattform(), anleitung.getQuelle(), bild);
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
