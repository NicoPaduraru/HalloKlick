package de.halloklick.anleitung;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.halloklick.familie.FamilienService;
import de.halloklick.gemeinsam.ApiFehler;

@Service
public class AnleitungService {

    /** Platzhalter für "gehört zu keiner Familie", weil eine leere Liste in SQL nicht erlaubt ist. */
    private static final UUID KEINE_FAMILIE = new UUID(0, 0);
    private static final int HOECHSTENS_PRO_FAMILIE = 50;

    private final AnleitungRepository anleitungen;
    private final FamilienService familien;
    private final Clock uhr;

    AnleitungService(AnleitungRepository anleitungen, FamilienService familien, Clock uhr) {
        this.anleitungen = anleitungen;
        this.familien = familien;
        this.uhr = uhr;
    }

    /**
     * Sucht Anleitungen. Sichtbar sind die allgemeinen Anleitungen und die eigenen
     * Anleitungen der Familien, zu denen das Konto gehört.
     *
     * @param kontoId darf null sein (nicht angemeldet): dann nur allgemeine Anleitungen
     * @param plattform darf null sein: dann alle Handys
     */
    @Transactional(readOnly = true)
    List<AnleitungDto.Kurz> suche(String suche, AnleitungPlattform plattform, UUID kontoId) {
        Collection<String> plattformen = plattform == null || plattform == AnleitungPlattform.ALLE
                ? List.of("ANDROID", "IOS", "ALLE")
                : List.of(plattform.name(), "ALLE");
        Collection<UUID> sichtbareFamilien = sichtbareFamilien(kontoId);

        String text = suche == null ? "" : suche.strip();
        if (text.length() > 100) {
            text = text.substring(0, 100);
        }
        List<UUID> ids = text.isEmpty()
                ? anleitungen.alle(plattformen, sichtbareFamilien)
                : anleitungen.suche(text, "%" + maskiere(text) + "%", plattformen, sichtbareFamilien);

        // Die Datenbank liefert die Reihenfolge, die Entities kommen danach in einem Rutsch
        Map<UUID, Anleitung> geladen = anleitungen.findAllById(ids).stream()
                .collect(Collectors.toMap(Anleitung::getId, Function.identity()));
        return ids.stream().map(geladen::get).filter(Objects::nonNull).map(AnleitungDto.Kurz::von).toList();
    }

    @Transactional(readOnly = true)
    AnleitungDto.Voll lade(UUID id, UUID kontoId) {
        Anleitung anleitung = anleitungen.findById(id).orElseThrow(AnleitungService::nichtGefunden);
        if (anleitung.getFamilieId() != null && !sichtbareFamilien(kontoId).contains(anleitung.getFamilieId())) {
            // Fremde Familien-Anleitung: so tun, als gäbe es sie nicht
            throw nichtGefunden();
        }
        return AnleitungDto.Voll.von(anleitung);
    }

    @Transactional(readOnly = true)
    List<AnleitungDto.Voll> anleitungenDerFamilie(UUID familieId, UUID kontoId) {
        familien.pruefeMitglied(kontoId, familieId);
        return anleitungen.findByFamilieIdOrderByTitel(familieId).stream().map(AnleitungDto.Voll::von).toList();
    }

    @Transactional
    AnleitungDto.Voll erstelle(UUID familieId, UUID kontoId, String titel, String kategorie,
            AnleitungPlattform plattform, List<String> schritte) {
        familien.pruefeMitglied(kontoId, familieId);
        if (anleitungen.countByFamilieId(familieId) >= HOECHSTENS_PRO_FAMILIE) {
            throw ApiFehler.konflikt("Eine Familie kann höchstens " + HOECHSTENS_PRO_FAMILIE
                    + " eigene Anleitungen haben.");
        }
        Anleitung anleitung = Anleitung.fuerFamilie(familieId, uhr.instant());
        anleitung.aendere(titel, kategorie, plattform, schritte);
        return AnleitungDto.Voll.von(anleitungen.save(anleitung));
    }

    @Transactional
    AnleitungDto.Voll aendere(UUID familieId, UUID anleitungId, UUID kontoId, String titel, String kategorie,
            AnleitungPlattform plattform, List<String> schritte) {
        Anleitung anleitung = eigeneAnleitung(familieId, anleitungId, kontoId);
        anleitung.aendere(titel, kategorie, plattform, schritte);
        return AnleitungDto.Voll.von(anleitung);
    }

    @Transactional
    void loesche(UUID familieId, UUID anleitungId, UUID kontoId) {
        anleitungen.delete(eigeneAnleitung(familieId, anleitungId, kontoId));
    }

    private Anleitung eigeneAnleitung(UUID familieId, UUID anleitungId, UUID kontoId) {
        familien.pruefeMitglied(kontoId, familieId);
        return anleitungen.findById(anleitungId)
                .filter(a -> familieId.equals(a.getFamilieId()))
                .orElseThrow(AnleitungService::nichtGefunden);
    }

    private Collection<UUID> sichtbareFamilien(UUID kontoId) {
        List<UUID> ids = new ArrayList<>();
        ids.add(KEINE_FAMILIE);
        if (kontoId != null) {
            ids.addAll(familien.familienIdsVon(kontoId));
        }
        return ids;
    }

    /** Die Zeichen % und _ haben in SQL-Mustern eine Sonderbedeutung und werden entschärft. */
    private static String maskiere(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static ApiFehler nichtGefunden() {
        return ApiFehler.nichtGefunden("Diese Anleitung gibt es nicht.");
    }
}
