package de.halloklick.benachrichtigung;

import java.net.URI;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.halloklick.gemeinsam.ApiFehler;
import de.halloklick.gemeinsam.DatenauskunftBeitrag;
import de.halloklick.gemeinsam.Einstellungen;
import de.halloklick.konto.KontoService;

/**
 * Benachrichtigt Angehörige: zuerst per Web Push, und wenn das nicht geht, per E-Mail.
 * Ist beides nicht eingerichtet, passiert nichts. Die Anfrage steht trotzdem in der App.
 */
@Service
public class BenachrichtigungsService implements DatenauskunftBeitrag {

    private static final Logger LOG = LoggerFactory.getLogger(BenachrichtigungsService.class);
    private static final int HOECHSTENS_ABOS = 10;

    /** Nur an diese bekannten Push-Dienste der Browser-Hersteller schickt der Server etwas. */
    private static final List<String> ERLAUBTE_PUSH_DIENSTE = List.of(
            "fcm.googleapis.com", ".push.services.mozilla.com", ".notify.windows.com", ".push.apple.com");

    private final PushAboRepository abos;
    private final WebPushSender push;
    private final ObjectProvider<JavaMailSender> mail;
    private final KontoService konten;
    private final Einstellungen einstellungen;
    private final ObjectMapper json;
    private final Clock uhr;

    BenachrichtigungsService(PushAboRepository abos, WebPushSender push, ObjectProvider<JavaMailSender> mail,
            KontoService konten, Einstellungen einstellungen, ObjectMapper json, Clock uhr) {
        this.abos = abos;
        this.push = push;
        this.mail = mail;
        this.konten = konten;
        this.einstellungen = einstellungen;
        this.json = json;
        this.uhr = uhr;
    }

    /** Läuft im Hintergrund, damit der Senior nicht auf das Verschicken warten muss. */
    @Async
    public void sende(Collection<UUID> kontoIds, String titel, String text) {
        for (UUID kontoId : kontoIds) {
            try {
                boolean angekommen = sendePush(kontoId, titel, text);
                if (!angekommen) {
                    sendeMail(kontoId, titel, text);
                }
            } catch (RuntimeException e) {
                LOG.warn("Benachrichtigung fehlgeschlagen: {}", e.getClass().getSimpleName());
            }
        }
    }

    private boolean sendePush(UUID kontoId, String titel, String text) {
        if (!push.eingerichtet()) {
            return false;
        }
        String inhalt;
        try {
            inhalt = json.writeValueAsString(Map.of("titel", titel, "text", text, "ziel", "/#/familie"));
        } catch (JsonProcessingException e) {
            return false;
        }
        boolean angekommen = false;
        for (PushAbo abo : abos.findByKontoId(kontoId)) {
            WebPushSender.Ergebnis ergebnis = push.sende(abo, inhalt);
            if (ergebnis == WebPushSender.Ergebnis.GESENDET) {
                angekommen = true;
            } else if (ergebnis == WebPushSender.Ergebnis.ABO_UNGUELTIG) {
                // Der Browser hat das Abo beendet: Die Adresse ist wertlos und wird entfernt
                abos.delete(abo);
            }
        }
        return angekommen;
    }

    private void sendeMail(UUID kontoId, String titel, String text) {
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null || !einstellungen.mail().eingerichtet()) {
            return;
        }
        konten.finde(kontoId).map(konto -> konto.email()).ifPresent(adresse -> {
            SimpleMailMessage nachricht = new SimpleMailMessage();
            nachricht.setFrom(einstellungen.mail().absender());
            nachricht.setTo(adresse);
            nachricht.setSubject("HalloKlick: " + titel);
            nachricht.setText(text + "\n\nÖffnen Sie die App, um zu helfen:\n"
                    + einstellungen.basisUrl().replaceAll("/+$", "") + "/#/familie\n");
            sender.send(nachricht);
        });
    }

    /** Speichert die Web-Push-Adresse eines Browsers für ein Konto. */
    @Transactional
    public void speichereAbo(UUID kontoId, String endpoint, String p256dh, String auth) {
        pruefeEndpoint(endpoint);
        var vorhanden = abos.findByEndpoint(endpoint);
        if (vorhanden.isPresent()) {
            vorhanden.get().uebernimm(kontoId, p256dh, auth);
            return;
        }
        if (abos.countByKontoId(kontoId) >= HOECHSTENS_ABOS) {
            throw ApiFehler.konflikt("Sie haben Benachrichtigungen schon auf sehr vielen Geräten angeschaltet.");
        }
        abos.save(new PushAbo(kontoId, endpoint, p256dh, auth, uhr.instant()));
    }

    @Transactional
    public void entferneAbo(UUID kontoId, String endpoint) {
        abos.findByEndpoint(endpoint).filter(abo -> abo.getKontoId().equals(kontoId)).ifPresent(abos::delete);
    }

    /**
     * Der Server ruft die Push-Adresse später selbst auf. Darum sind nur die echten
     * Push-Dienste erlaubt. Sonst könnte jemand den Server Anfragen an beliebige
     * Adressen schicken lassen.
     */
    private static void pruefeEndpoint(String endpoint) {
        try {
            URI adresse = URI.create(endpoint);
            String host = adresse.getHost();
            boolean erlaubt = "https".equals(adresse.getScheme()) && host != null && adresse.getUserInfo() == null
                    && ERLAUBTE_PUSH_DIENSTE.stream().anyMatch(dienst -> dienst.startsWith(".")
                            ? host.endsWith(dienst) : host.equals(dienst));
            if (erlaubt) {
                return;
            }
        } catch (IllegalArgumentException e) {
            // unten als ungültig behandelt
        }
        throw ApiFehler.ungueltig("Diese Adresse für Benachrichtigungen kennen wir nicht.");
    }

    // ---------- Datenauskunft ----------

    @Override
    public String name() {
        return "benachrichtigungen";
    }

    @Override
    @Transactional(readOnly = true)
    public Object datenVon(UUID kontoId) {
        return abos.findByKontoId(kontoId).stream()
                .map(abo -> Map.of("endpoint", abo.getEndpoint(), "erstelltAm", abo.getErstelltAm()))
                .toList();
    }
}
