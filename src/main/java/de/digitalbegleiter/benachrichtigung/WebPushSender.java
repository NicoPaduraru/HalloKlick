package de.digitalbegleiter.benachrichtigung;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.digitalbegleiter.gemeinsam.Einstellungen;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

/**
 * Schickt eine verschlüsselte Web-Push-Nachricht an einen Browser.
 * Dafür braucht der Server ein Schlüsselpaar (VAPID), siehe docs/BETRIEB.md.
 */
@Component
class WebPushSender {

    enum Ergebnis {
        GESENDET,
        /** Der Browser hat das Abo beendet. Die Adresse kann weg. */
        ABO_UNGUELTIG,
        FEHLER
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebPushSender.class);

    private final PushService pushService;

    WebPushSender(Einstellungen einstellungen) {
        PushService dienst = null;
        Einstellungen.Push push = einstellungen.push();
        if (push.eingerichtet()) {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                dienst = new PushService(push.oeffentlicherSchluessel(), push.privaterSchluessel(), push.betreff());
            } catch (Exception e) {
                LOG.error("Web Push ist falsch eingerichtet ({}). Es werden keine Push-Nachrichten verschickt.",
                        e.getClass().getSimpleName());
            }
        }
        this.pushService = dienst;
    }

    boolean eingerichtet() {
        return pushService != null;
    }

    Ergebnis sende(PushAbo abo, String inhaltAlsJson) {
        if (pushService == null) {
            return Ergebnis.FEHLER;
        }
        try {
            var antwort = pushService.send(
                    new Notification(abo.getEndpoint(), abo.getP256dh(), abo.getAuth(), inhaltAlsJson));
            int status = antwort.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                return Ergebnis.ABO_UNGUELTIG;
            }
            return status >= 200 && status < 300 ? Ergebnis.GESENDET : Ergebnis.FEHLER;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Ergebnis.FEHLER;
        } catch (Exception e) {
            LOG.warn("Web Push fehlgeschlagen: {}", e.getClass().getSimpleName());
            return Ergebnis.FEHLER;
        }
    }
}
