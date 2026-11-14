package de.halloklick.familie;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import de.halloklick.benachrichtigung.BenachrichtigungsService;
import de.halloklick.gemeinsam.ApiFehler;
import de.halloklick.gemeinsam.DatenauskunftBeitrag;
import de.halloklick.gemeinsam.Einstellungen;
import de.halloklick.gemeinsam.Texte;
import de.halloklick.konto.KontoService;

/** "Familie fragen": Der Senior bittet um Hilfe, die Angehörigen sehen und erledigen die Anfrage. */
@Service
public class HilfeanfrageService implements DatenauskunftBeitrag {

    private static final Logger LOG = LoggerFactory.getLogger(HilfeanfrageService.class);

    /** Hilfeanfragen werden nach 30 Tagen gelöscht (Bauplan, Abschnitt 9). */
    static final Duration LOESCHFRIST = Duration.ofDays(30);
    private static final int HOECHSTENS_PRO_TAG = 20;

    public record HilfeanfrageErstellt(UUID familieId, String vonName, HilfeThema thema) {
    }

    private final HilfeanfrageRepository anfragen;
    private final FamilienService familien;
    private final MitgliedschaftRepository mitgliedschaften;
    private final KontoService konten;
    private final BenachrichtigungsService benachrichtigung;
    private final ApplicationEventPublisher ereignisse;
    private final Einstellungen einstellungen;
    private final Clock uhr;
    private final Texte texte;

    HilfeanfrageService(HilfeanfrageRepository anfragen, FamilienService familien,
            MitgliedschaftRepository mitgliedschaften, KontoService konten,
            BenachrichtigungsService benachrichtigung, ApplicationEventPublisher ereignisse,
            Einstellungen einstellungen, Clock uhr, Texte texte) {
        this.texte = texte;
        this.anfragen = anfragen;
        this.familien = familien;
        this.mitgliedschaften = mitgliedschaften;
        this.konten = konten;
        this.benachrichtigung = benachrichtigung;
        this.ereignisse = ereignisse;
        this.einstellungen = einstellungen;
        this.uhr = uhr;
    }

    /**
     * Schickt eine Hilfeanfrage an alle Familien des Seniors.
     *
     * @param geteilterText nur gesetzt, wenn der Senior den Inhalt ausdrücklich mitschicken will
     * @return an wie viele Familien die Anfrage ging
     */
    @Transactional
    public int erstelle(UUID seniorKontoId, HilfeThema thema, String geteilterText) {
        if (!einstellungen.funktionen().familie()) {
            throw ApiFehler.abgeschaltet();
        }
        List<UUID> familienIds = mitgliedschaften.findByIdKontoId(seniorKontoId).stream()
                .filter(m -> m.getRolle() == Rolle.SENIOR)
                .map(Mitgliedschaft::getFamilieId)
                .toList();
        if (familienIds.isEmpty()) {
            throw ApiFehler.konflikt("Sie sind noch nicht mit Ihrer Familie verbunden.");
        }
        Instant jetzt = uhr.instant();
        if (anfragen.countByVonKontoIdAndErstelltAmAfter(seniorKontoId, jetzt.minus(Duration.ofDays(1)))
                >= HOECHSTENS_PRO_TAG) {
            throw new ApiFehler(HttpStatus.TOO_MANY_REQUESTS,
                    "Sie haben heute schon sehr viele Anfragen geschickt. Bitte rufen Sie Ihre Familie an.");
        }
        String text = geteilterText == null || geteilterText.isBlank() ? null : geteilterText.strip();
        String vonName = konten.anzeigenameVon(seniorKontoId);
        for (UUID familieId : familienIds) {
            anfragen.save(new Hilfeanfrage(seniorKontoId, familieId, thema, text, jetzt));
            ereignisse.publishEvent(new HilfeanfrageErstellt(familieId, vonName, thema));
        }
        return familienIds.size();
    }

    /** Erst wenn die Anfrage sicher gespeichert ist, gehen die Benachrichtigungen raus. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void benachrichtigeAngehoerige(HilfeanfrageErstellt ereignis) {
        // In der Benachrichtigung steht nur, WER fragt und das Thema. Nie der geteilte Inhalt.
        benachrichtigung.sende(familien.angehoerigeVon(ereignis.familieId()),
                "Neue Hilfeanfrage", ereignis.vonName() + ": ", ereignis.thema().text());
    }

    @Transactional(readOnly = true)
    public List<FamilieDto.Anfrage> anfragenDerFamilie(UUID familieId, UUID kontoId, HilfeStatus status) {
        familien.pruefeMitglied(kontoId, familieId);
        List<Hilfeanfrage> gefunden = status == null
                ? anfragen.findByFamilieIdOrderByErstelltAmDesc(familieId)
                : anfragen.findByFamilieIdAndStatusOrderByErstelltAmDesc(familieId, status);
        return gefunden.stream().map(this::alsDto).toList();
    }

    @Transactional
    public FamilieDto.Anfrage setzeStatus(UUID anfrageId, UUID kontoId, HilfeStatus status) {
        Hilfeanfrage anfrage = anfragen.findById(anfrageId)
                .orElseThrow(() -> ApiFehler.nichtGefunden("Diese Anfrage gibt es nicht."));
        try {
            familien.pruefeMitglied(kontoId, anfrage.getFamilieId());
        } catch (ApiFehler keinMitglied) {
            // Fremde Anfrage: so tun, als gäbe es sie nicht
            throw ApiFehler.nichtGefunden("Diese Anfrage gibt es nicht.");
        }
        anfrage.setStatus(status, uhr.instant());
        return alsDto(anfrage);
    }

    @Transactional
    public int loescheAlte(Instant jetzt) {
        return anfragen.loescheAelterAls(jetzt.minus(LOESCHFRIST));
    }

    @Scheduled(cron = "0 0 3 * * *")
    void naechtlichesAufraeumen() {
        int anzahl = loescheAlte(uhr.instant());
        if (anzahl > 0) {
            LOG.info("{} Hilfeanfragen gelöscht (Löschfrist 30 Tage)", anzahl);
        }
    }

    private FamilieDto.Anfrage alsDto(Hilfeanfrage anfrage) {
        return new FamilieDto.Anfrage(anfrage.getId(), anfrage.getFamilieId(),
                texte.hier(konten.anzeigenameVon(anfrage.getVonKontoId())), anfrage.getThema(),
                texte.hier(anfrage.getThema().text()),
                anfrage.getGeteilterText(), anfrage.getStatus(), anfrage.getErstelltAm());
    }

    // ---------- Datenauskunft ----------

    @Override
    public String name() {
        return "hilfeanfragen";
    }

    @Override
    @Transactional(readOnly = true)
    public Object datenVon(UUID kontoId) {
        return anfragen.findByVonKontoIdOrderByErstelltAmDesc(kontoId).stream()
                .map(a -> {
                    Map<String, Object> daten = new LinkedHashMap<>();
                    daten.put("erstelltAm", a.getErstelltAm());
                    daten.put("thema", a.getThema());
                    daten.put("geteilterText", a.getGeteilterText());
                    daten.put("status", a.getStatus());
                    return daten;
                })
                .toList();
    }
}
