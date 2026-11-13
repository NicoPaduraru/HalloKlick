package de.halloklick.konto;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.halloklick.gemeinsam.ApiFehler;

/** Öffentliche Tür des Moduls "konto". Andere Module sprechen nur mit dieser Klasse. */
@Service
public class KontoService {

    private final KontoRepository konten;
    private final PasswordEncoder passwortHasher;
    private final ApplicationEventPublisher ereignisse;
    private final Clock uhr;
    /** Wird geprüft, wenn es die E-Mail nicht gibt. So dauert die Antwort immer gleich lang. */
    private final String attrappenHash;

    KontoService(KontoRepository konten, PasswordEncoder passwortHasher, ApplicationEventPublisher ereignisse,
            Clock uhr) {
        this.konten = konten;
        this.passwortHasher = passwortHasher;
        this.ereignisse = ereignisse;
        this.uhr = uhr;
        this.attrappenHash = passwortHasher.encode("nur-eine-attrappe");
    }

    @Transactional
    public KontoInfo registriereAngehoerigen(String email, String passwort, String anzeigename) {
        String adresse = normalisiere(email);
        if (konten.existsByEmail(adresse)) {
            throw ApiFehler.konflikt("Mit dieser E-Mail-Adresse gibt es schon ein Konto.");
        }
        Konto konto = Konto.neuerAngehoeriger(adresse, passwortHasher.encode(passwort), anzeigename.strip(),
                uhr.instant());
        return KontoInfo.von(konten.save(konto));
    }

    @Transactional(readOnly = true)
    public KontoInfo pruefeAnmeldung(String email, String passwort) {
        Optional<Konto> konto = konten.findByEmail(normalisiere(email));
        String hash = konto.map(Konto::getPasswortHash).orElse(attrappenHash);
        boolean stimmt = passwortHasher.matches(passwort, hash);
        if (konto.isEmpty() || !stimmt) {
            // Absichtlich derselbe Text für "E-Mail unbekannt" und "Passwort falsch"
            throw new ApiFehler(HttpStatus.UNAUTHORIZED, "E-Mail-Adresse oder Passwort stimmen nicht.");
        }
        return KontoInfo.von(konto.get());
    }

    @Transactional
    UUID erstelleSeniorKonto() {
        return konten.save(Konto.neuerSenior(uhr.instant())).getId();
    }

    @Transactional(readOnly = true)
    public Optional<KontoInfo> finde(UUID kontoId) {
        return konten.findById(kontoId).map(KontoInfo::von);
    }

    /** Name für die Anzeige. Senioren ohne Namen heißen einfach "Ohne Namen". */
    @Transactional(readOnly = true)
    public String anzeigenameVon(UUID kontoId) {
        return konten.findById(kontoId).map(Konto::getAnzeigename).filter(n -> !n.isBlank()).orElse("Ohne Namen");
    }

    @Transactional
    public void setzeAnzeigename(UUID kontoId, String anzeigename) {
        konten.findById(kontoId).ifPresent(konto -> konto.setAnzeigename(anzeigename.strip()));
    }

    /** Löscht das Konto und alles, was daran hängt (Geräte, Checks, Anfragen, Mitgliedschaften). */
    @Transactional
    public void loesche(UUID kontoId) {
        if (!konten.existsById(kontoId)) {
            return;
        }
        ereignisse.publishEvent(new KontoWirdGeloeschtEvent(kontoId));
        konten.deleteById(kontoId);
    }

    private static String normalisiere(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
