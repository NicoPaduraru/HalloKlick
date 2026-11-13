package de.digitalbegleiter.konto;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "geraet")
class Geraet {

    @Id
    private UUID id;

    @Column(name = "konto_id", nullable = false)
    private UUID kontoId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plattform plattform;

    @Column(name = "zuletzt_gesehen", nullable = false)
    private Instant zuletztGesehen;

    @Column(nullable = false)
    private boolean widerrufen;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected Geraet() {
        // für JPA
    }

    Geraet(UUID kontoId, String tokenHash, Plattform plattform, Instant jetzt) {
        this.id = UUID.randomUUID();
        this.kontoId = kontoId;
        this.tokenHash = tokenHash;
        this.plattform = plattform;
        this.zuletztGesehen = jetzt;
        this.erstelltAm = jetzt;
    }

    UUID getId() {
        return id;
    }

    UUID getKontoId() {
        return kontoId;
    }

    Plattform getPlattform() {
        return plattform;
    }

    Instant getZuletztGesehen() {
        return zuletztGesehen;
    }

    void setZuletztGesehen(Instant zuletztGesehen) {
        this.zuletztGesehen = zuletztGesehen;
    }

    boolean isWiderrufen() {
        return widerrufen;
    }

    void widerrufe() {
        this.widerrufen = true;
    }

    Instant getErstelltAm() {
        return erstelltAm;
    }
}
