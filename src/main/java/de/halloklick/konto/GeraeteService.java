package de.halloklick.konto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.halloklick.gemeinsam.Schluessel;
import de.halloklick.gemeinsam.sicherheit.Angemeldet;
import de.halloklick.gemeinsam.sicherheit.GeraeteAnmelder;

/**
 * Geräte sind der Login der Senioren: kein Passwort, nur ein langer, zufälliger
 * Schlüssel im Cookie. In der Datenbank liegt nur der Hash.
 */
@Service
public class GeraeteService implements GeraeteAnmelder {

    private static final Logger LOG = LoggerFactory.getLogger(GeraeteService.class);

    /** Inaktive Geräte werden nach 12 Monaten gelöscht (Bauplan, Abschnitt 9). */
    static final Duration LOESCHFRIST = Duration.ofDays(365);

    /** Der Schlüssel wird nur ein einziges Mal herausgegeben, direkt nach dem Anlegen. */
    public record NeuesGeraet(UUID kontoId, UUID geraetId, String schluessel) {
    }

    private final GeraetRepository geraete;
    private final KontoService konten;
    private final Clock uhr;

    GeraeteService(GeraetRepository geraete, KontoService konten, Clock uhr) {
        this.geraete = geraete;
        this.konten = konten;
        this.uhr = uhr;
    }

    /** Legt ein anonymes Senioren-Konto mit einem Gerät an. Nutzbar auch ohne Familie. */
    @Transactional
    public NeuesGeraet erstelleAnonymesGeraet(Plattform plattform) {
        UUID kontoId = konten.erstelleSeniorKonto();
        String schluessel = Schluessel.neuerToken();
        Geraet geraet = geraete.save(new Geraet(kontoId, Schluessel.sha256(schluessel), plattform, uhr.instant()));
        return new NeuesGeraet(kontoId, geraet.getId(), schluessel);
    }

    @Override
    @Transactional
    public Optional<Angemeldet> anmelden(String geraeteSchluessel) {
        if (geraeteSchluessel == null || geraeteSchluessel.length() > 100) {
            return Optional.empty();
        }
        return geraete.findByTokenHash(Schluessel.sha256(geraeteSchluessel))
                .filter(geraet -> !geraet.isWiderrufen())
                .map(geraet -> {
                    // Höchstens einmal am Tag schreiben, das reicht für die Löschfrist
                    Instant jetzt = uhr.instant();
                    if (geraet.getZuletztGesehen().isBefore(jetzt.minus(Duration.ofDays(1)))) {
                        geraet.setZuletztGesehen(jetzt);
                    }
                    return new Angemeldet(geraet.getKontoId(), Angemeldet.SENIOR, geraet.getId());
                });
    }

    @Transactional(readOnly = true)
    public List<GeraetInfo> geraeteVon(UUID kontoId) {
        return geraete.findByKontoIdOrderByErstelltAm(kontoId).stream().map(GeraetInfo::von).toList();
    }

    @Transactional(readOnly = true)
    public Optional<GeraetInfo> finde(UUID geraetId) {
        return geraete.findById(geraetId).map(GeraetInfo::von);
    }

    /** Sperrt ein verlorenes Handy. Der Schlüssel auf dem Gerät ist danach wertlos. */
    @Transactional
    public void sperre(UUID geraetId) {
        geraete.findById(geraetId).ifPresent(Geraet::widerrufe);
    }

    /** Löscht Senioren-Konten, deren Geräte seit 12 Monaten nicht mehr benutzt wurden. */
    @Transactional
    public int loescheInaktive(Instant jetzt) {
        Instant grenze = jetzt.minus(LOESCHFRIST);
        List<UUID> kandidaten = geraete.findByZuletztGesehenBefore(grenze).stream()
                .map(Geraet::getKontoId)
                .distinct()
                .filter(kontoId -> !geraete.existsByKontoIdAndZuletztGesehenGreaterThanEqual(kontoId, grenze))
                .toList();
        kandidaten.forEach(konten::loesche);
        return kandidaten.size();
    }

    @Scheduled(cron = "0 30 3 * * *")
    void naechtlichesAufraeumen() {
        int anzahl = loescheInaktive(uhr.instant());
        if (anzahl > 0) {
            LOG.info("{} inaktive Senioren-Konten gelöscht (Löschfrist 12 Monate)", anzahl);
        }
    }
}
