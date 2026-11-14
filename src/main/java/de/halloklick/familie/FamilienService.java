package de.halloklick.familie;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.halloklick.gemeinsam.ApiFehler;
import de.halloklick.gemeinsam.DatenauskunftBeitrag;
import de.halloklick.gemeinsam.Einstellungen;
import de.halloklick.gemeinsam.Schluessel;
import de.halloklick.gemeinsam.Texte;
import de.halloklick.konto.GeraetInfo;
import de.halloklick.konto.GeraeteService;
import de.halloklick.konto.KontoService;
import de.halloklick.konto.KontoWirdGeloeschtEvent;

/** Öffentliche Tür des Moduls "familie": Familien, Mitglieder, Einladungen, Geräte koppeln und sperren. */
@Service
public class FamilienService implements DatenauskunftBeitrag {

    private static final Logger LOG = LoggerFactory.getLogger(FamilienService.class);

    /** Eine Einladung gilt 15 Minuten (Bauplan, Abschnitt 5). */
    static final Duration EINLADUNG_GUELTIG = Duration.ofMinutes(15);
    private static final int CODE_LAENGE = 8;
    private static final int HOECHSTENS_FAMILIEN = 5;
    private static final int HOECHSTENS_MITGLIEDER = 20;

    private final FamilieRepository familien;
    private final MitgliedschaftRepository mitgliedschaften;
    private final EinladungRepository einladungen;
    private final KontoService konten;
    private final GeraeteService geraete;
    private final Einstellungen einstellungen;
    private final Clock uhr;
    private final Texte texte;

    FamilienService(FamilieRepository familien, MitgliedschaftRepository mitgliedschaften,
            EinladungRepository einladungen, KontoService konten, GeraeteService geraete,
            Einstellungen einstellungen, Clock uhr, Texte texte) {
        this.texte = texte;
        this.familien = familien;
        this.mitgliedschaften = mitgliedschaften;
        this.einladungen = einladungen;
        this.konten = konten;
        this.geraete = geraete;
        this.einstellungen = einstellungen;
        this.uhr = uhr;
    }

    // ---------- Fragen anderer Module ----------

    @Transactional(readOnly = true)
    public List<UUID> familienIdsVon(UUID kontoId) {
        return mitgliedschaften.findByIdKontoId(kontoId).stream().map(Mitgliedschaft::getFamilieId).toList();
    }

    /**
     * Rechteprüfung: Nur Mitglieder sehen Daten ihrer Familie. Für Fremde sieht es so aus,
     * als gäbe es die Familie gar nicht (404 statt 403).
     */
    @Transactional(readOnly = true)
    public Rolle pruefeMitglied(UUID kontoId, UUID familieId) {
        return mitgliedschaften.findById(new MitgliedschaftId(familieId, kontoId))
                .map(Mitgliedschaft::getRolle)
                .orElseThrow(() -> ApiFehler.nichtGefunden("Diese Familie gibt es nicht."));
    }

    @Transactional(readOnly = true)
    public void pruefeVerwalter(UUID kontoId, UUID familieId) {
        if (pruefeMitglied(kontoId, familieId) != Rolle.VERWALTER) {
            throw ApiFehler.keinZugriff();
        }
    }

    /** Alle Angehörigen einer Familie, zum Beispiel für Benachrichtigungen. */
    @Transactional(readOnly = true)
    public List<UUID> angehoerigeVon(UUID familieId) {
        return mitgliedschaften.findByIdFamilieIdOrderByBeigetretenAm(familieId).stream()
                .filter(m -> m.getRolle() != Rolle.SENIOR)
                .map(Mitgliedschaft::getKontoId)
                .toList();
    }

    // ---------- Familie ----------

    @Transactional
    public FamilieDto.Kurz erstelle(UUID kontoId, String name) {
        pruefeAngeschaltet();
        if (mitgliedschaften.countByIdKontoIdAndRolle(kontoId, Rolle.VERWALTER) >= HOECHSTENS_FAMILIEN) {
            throw ApiFehler.konflikt("Sie können höchstens " + HOECHSTENS_FAMILIEN + " Familien anlegen.");
        }
        Instant jetzt = uhr.instant();
        Familie familie = familien.save(new Familie(name.strip(), kontoId, jetzt));
        mitgliedschaften.save(new Mitgliedschaft(familie.getId(), kontoId, Rolle.VERWALTER, jetzt));
        return new FamilieDto.Kurz(familie.getId(), familie.getName(), Rolle.VERWALTER);
    }

    @Transactional(readOnly = true)
    public List<FamilieDto.Kurz> familienVon(UUID kontoId) {
        return mitgliedschaften.findByIdKontoId(kontoId).stream()
                .map(m -> familien.findById(m.getFamilieId())
                        .map(f -> new FamilieDto.Kurz(f.getId(), f.getName(), m.getRolle()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilieDto.Details details(UUID familieId, UUID kontoId) {
        Rolle meineRolle = pruefeMitglied(kontoId, familieId);
        Familie familie = familien.findById(familieId)
                .orElseThrow(() -> ApiFehler.nichtGefunden("Diese Familie gibt es nicht."));
        List<FamilieDto.Mitglied> mitglieder = mitgliedschaften.findByIdFamilieIdOrderByBeigetretenAm(familieId)
                .stream()
                .map(m -> new FamilieDto.Mitglied(m.getKontoId(), texte.hier(konten.anzeigenameVon(m.getKontoId())),
                        m.getRolle(), m.getRolle() == Rolle.SENIOR ? geraete.geraeteVon(m.getKontoId()) : List.of()))
                .toList();
        return new FamilieDto.Details(familie.getId(), familie.getName(), meineRolle, mitglieder);
    }

    // ---------- Einladung und Koppeln ----------

    /** Erzeugt einen Einmal-Code samt QR-Code. Nur der Verwalter darf einladen. */
    @Transactional
    public FamilieDto.NeueEinladung erstelleEinladung(UUID familieId, UUID kontoId, String nameFuerSenior) {
        pruefeAngeschaltet();
        pruefeVerwalter(kontoId, familieId);
        Instant jetzt = uhr.instant();
        Instant gueltigBis = jetzt.plus(EINLADUNG_GUELTIG);
        String code = Schluessel.neuerCode(CODE_LAENGE);
        String name = nameFuerSenior == null || nameFuerSenior.isBlank() ? null : nameFuerSenior.strip();
        einladungen.save(new Einladung(familieId, Schluessel.sha256(code), name, gueltigBis, jetzt));
        String link = einstellungen.basisUrl().replaceAll("/+$", "") + "/#/verbinden/" + code;
        return new FamilieDto.NeueEinladung(code, link, gueltigBis, QrCode.alsSvg(link));
    }

    /** Löst einen Einladungscode ein und verbindet das Senioren-Konto mit der Familie. */
    @Transactional
    public FamilieDto.Kurz koppeln(UUID seniorKontoId, String eingabe) {
        pruefeAngeschaltet();
        // Großschreibung, Leerzeichen und Bindestriche sind beim Abtippen egal
        String code = eingabe == null ? "" : eingabe.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        Instant jetzt = uhr.instant();
        Einladung einladung = einladungen.findByCodeHash(Schluessel.sha256(code)).orElse(null);
        // Derselbe Text für "falsch", "abgelaufen" und "schon benutzt": Niemand soll Codes ausforschen können
        if (einladung == null || einladungen.loeseEin(einladung.getId(), jetzt) != 1) {
            throw ApiFehler.nichtGefunden("Dieser Code ist nicht gültig oder abgelaufen. "
                    + "Bitten Sie Ihre Familie um einen neuen Code.");
        }
        UUID familieId = einladung.getFamilieId();
        Familie familie = familien.findById(familieId)
                .orElseThrow(() -> ApiFehler.nichtGefunden("Diese Familie gibt es nicht."));
        MitgliedschaftId id = new MitgliedschaftId(familieId, seniorKontoId);
        if (!mitgliedschaften.existsById(id)) {
            if (mitgliedschaften.countByIdFamilieId(familieId) >= HOECHSTENS_MITGLIEDER) {
                throw ApiFehler.konflikt("Diese Familie hat schon zu viele Mitglieder.");
            }
            mitgliedschaften.save(new Mitgliedschaft(familieId, seniorKontoId, Rolle.SENIOR, jetzt));
        }
        if (einladung.getNameFuerSenior() != null) {
            konten.setzeAnzeigename(seniorKontoId, einladung.getNameFuerSenior());
        }
        return new FamilieDto.Kurz(familieId, familie.getName(), Rolle.SENIOR);
    }

    /**
     * Sperrt ein verlorenes Handy. Erlaubt ist das nur dem Verwalter einer Familie,
     * zu der das Gerät gehört.
     */
    @Transactional
    public void sperreGeraet(UUID kontoId, UUID geraetId) {
        GeraetInfo geraet = geraete.finde(geraetId).orElseThrow(FamilienService::geraetNichtGefunden);
        boolean erlaubt = mitgliedschaften.findByIdKontoId(geraet.kontoId()).stream()
                .filter(m -> m.getRolle() == Rolle.SENIOR)
                .anyMatch(m -> mitgliedschaften.findById(new MitgliedschaftId(m.getFamilieId(), kontoId))
                        .filter(meine -> meine.getRolle() == Rolle.VERWALTER)
                        .isPresent());
        if (!erlaubt) {
            throw geraetNichtGefunden();
        }
        geraete.sperre(geraetId);
    }

    // ---------- Aufräumen ----------

    /** Verlässt der letzte Angehörige eine Familie, wird die Familie mit gelöscht. */
    @EventListener
    @Transactional
    public void wennKontoGeloeschtWird(KontoWirdGeloeschtEvent ereignis) {
        for (Mitgliedschaft meine : mitgliedschaften.findByIdKontoId(ereignis.kontoId())) {
            if (meine.getRolle() == Rolle.SENIOR) {
                continue;
            }
            boolean andereAngehoerige = mitgliedschaften.findByIdFamilieIdOrderByBeigetretenAm(meine.getFamilieId())
                    .stream()
                    .anyMatch(m -> m.getRolle() != Rolle.SENIOR && !m.getKontoId().equals(ereignis.kontoId()));
            if (!andereAngehoerige) {
                familien.deleteById(meine.getFamilieId());
            }
        }
    }

    @Transactional
    public int loescheAbgelaufeneEinladungen(Instant jetzt) {
        return einladungen.loescheAbgelaufene(jetzt.minus(Duration.ofDays(1)));
    }

    @Scheduled(cron = "0 15 3 * * *")
    void naechtlichesAufraeumen() {
        int anzahl = loescheAbgelaufeneEinladungen(uhr.instant());
        if (anzahl > 0) {
            LOG.info("{} abgelaufene Einladungen gelöscht", anzahl);
        }
    }

    private void pruefeAngeschaltet() {
        if (!einstellungen.funktionen().familie()) {
            throw ApiFehler.abgeschaltet();
        }
    }

    private static ApiFehler geraetNichtGefunden() {
        return ApiFehler.nichtGefunden("Dieses Gerät gibt es nicht.");
    }

    // ---------- Datenauskunft ----------

    @Override
    public String name() {
        return "familien";
    }

    @Override
    @Transactional(readOnly = true)
    public Object datenVon(UUID kontoId) {
        return mitgliedschaften.findByIdKontoId(kontoId).stream()
                .map(m -> Map.of(
                        "familie", familien.findById(m.getFamilieId()).map(Familie::getName).orElse(""),
                        "rolle", m.getRolle(),
                        "beigetretenAm", m.getBeigetretenAm()))
                .toList();
    }
}
