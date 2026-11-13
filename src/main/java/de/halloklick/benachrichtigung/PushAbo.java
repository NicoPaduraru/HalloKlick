package de.halloklick.benachrichtigung;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Die Web-Push-Adresse eines Browsers, an die wir Benachrichtigungen schicken dürfen. */
@Entity
@Table(name = "push_abo")
class PushAbo {

    @Id
    private UUID id;

    @Column(name = "konto_id", nullable = false)
    private UUID kontoId;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected PushAbo() {
        // für JPA
    }

    PushAbo(UUID kontoId, String endpoint, String p256dh, String auth, Instant jetzt) {
        this.id = UUID.randomUUID();
        this.kontoId = kontoId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.erstelltAm = jetzt;
    }

    void uebernimm(UUID kontoId, String p256dh, String auth) {
        this.kontoId = kontoId;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    UUID getId() {
        return id;
    }

    UUID getKontoId() {
        return kontoId;
    }

    String getEndpoint() {
        return endpoint;
    }

    String getP256dh() {
        return p256dh;
    }

    String getAuth() {
        return auth;
    }

    Instant getErstelltAm() {
        return erstelltAm;
    }
}
