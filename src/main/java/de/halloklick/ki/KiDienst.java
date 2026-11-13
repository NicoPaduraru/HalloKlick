package de.halloklick.ki;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.halloklick.gemeinsam.DatenauskunftBeitrag;
import de.halloklick.gemeinsam.Einstellungen;

/**
 * Öffentliche Tür des Moduls "ki" mit der Kostenbremse:
 * <ol>
 *   <li>Pro Person höchstens N KI-Checks am Tag</li>
 *   <li>Monatsbudget für alle zusammen</li>
 * </ol>
 * (Ebene "Regeln zuerst" liegt im Modul "check", das Ausgabenlimit beim Anbieter
 * stellt man in der Anthropic Console ein.)
 */
@Service
public class KiDienst implements DatenauskunftBeitrag {

    private static final Logger LOG = LoggerFactory.getLogger(KiDienst.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    /** Eine Zeile der internen Übersicht. */
    public record TagesKosten(LocalDate datum, int aufrufe, long eingabeTokens, long ausgabeTokens,
            BigDecimal geschaetzteKostenUsd) {
    }

    public record Uebersicht(String modell, boolean einsatzbereit, BigDecimal monatsbudgetUsd,
            BigDecimal kostenDiesenMonatUsd, List<TagesKosten> tage) {
    }

    private final NachrichtenPruefer pruefer;
    private final NutzungRepository nutzung;
    private final Einstellungen.Ki ki;
    private final Clock uhr;

    KiDienst(NachrichtenPruefer pruefer, NutzungRepository nutzung, Einstellungen einstellungen, Clock uhr) {
        this.pruefer = pruefer;
        this.nutzung = nutzung;
        this.ki = einstellungen.ki();
        this.uhr = uhr;
        if (ki.aktiv() && !ki.einsatzbereit()) {
            LOG.warn("KI ist angeschaltet, aber es fehlt der API-Schlüssel oder das Monatsbudget. "
                    + "Es läuft nur die Regel-Prüfung.");
        }
    }

    /** Fragt die KI, wenn die Kostenbremse es erlaubt. Wirft nie eine Ausnahme. */
    public KiErgebnis pruefe(UUID kontoId, PruefEingabe eingabe) {
        if (!ki.einsatzbereit()) {
            return KiErgebnis.ohne(KiErgebnis.Grund.ABGESCHALTET);
        }
        LocalDate heute = LocalDate.now(uhr.withZone(ZONE));
        if (kostenSeit(heute.withDayOfMonth(1)).compareTo(ki.monatsbudgetUsd()) >= 0) {
            return KiErgebnis.ohne(KiErgebnis.Grund.MONATSBUDGET);
        }
        if (!nutzung.reserviere(kontoId, heute, ki.tageslimit())) {
            return KiErgebnis.ohne(KiErgebnis.Grund.TAGESLIMIT);
        }
        try {
            KiEinschaetzung einschaetzung = pruefer.pruefe(eingabe);
            nutzung.verbuche(heute, einschaetzung.eingabeTokens(), einschaetzung.ausgabeTokens());
            return KiErgebnis.ok(einschaetzung);
        } catch (KiNichtVerfuegbarException e) {
            return KiErgebnis.ohne(e.grund());
        } catch (RuntimeException e) {
            LOG.warn("Unerwarteter Fehler beim KI-Aufruf: {}", e.getClass().getSimpleName());
            return KiErgebnis.ohne(KiErgebnis.Grund.FEHLER);
        }
    }

    /** Interne Übersicht: KI-Aufrufe und geschätzte Kosten pro Tag (letzte 60 Tage). */
    public Uebersicht uebersicht() {
        LocalDate heute = LocalDate.now(uhr.withZone(ZONE));
        List<TagesKosten> tage = nutzung.verbrauchSeit(heute.minusDays(60)).stream()
                .map(t -> new TagesKosten(t.datum(), t.aufrufe(), t.eingabeTokens(), t.ausgabeTokens(),
                        kosten(t.eingabeTokens(), t.ausgabeTokens())))
                .toList();
        return new Uebersicht(ki.modell(), ki.einsatzbereit(), ki.monatsbudgetUsd(),
                kostenSeit(heute.withDayOfMonth(1)), tage);
    }

    private BigDecimal kostenSeit(LocalDate von) {
        return nutzung.verbrauchSeit(von).stream()
                .map(t -> kosten(t.eingabeTokens(), t.ausgabeTokens()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal kosten(long eingabeTokens, long ausgabeTokens) {
        BigDecimal eingabe = ki.preisEingabeProMio().multiply(BigDecimal.valueOf(eingabeTokens));
        BigDecimal ausgabe = ki.preisAusgabeProMio().multiply(BigDecimal.valueOf(ausgabeTokens));
        return eingabe.add(ausgabe).divide(MILLION, 4, RoundingMode.HALF_UP);
    }

    // --- Datenauskunft ---

    @Override
    public String name() {
        return "kiNutzung";
    }

    @Override
    public Object datenVon(UUID kontoId) {
        return nutzung.nutzungVon(kontoId);
    }
}
