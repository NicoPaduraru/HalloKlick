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
@Table(name = "konto")
class Konto {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KontoTyp typ;

    private String anzeigename;

    private String email;

    @Column(name = "passwort_hash")
    private String passwortHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tarif tarif;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected Konto() {
        // für JPA
    }

    static Konto neuerSenior(Instant jetzt) {
        Konto konto = new Konto();
        konto.id = UUID.randomUUID();
        konto.typ = KontoTyp.SENIOR;
        konto.tarif = Tarif.KOSTENLOS;
        konto.erstelltAm = jetzt;
        return konto;
    }

    static Konto neuerAngehoeriger(String email, String passwortHash, String anzeigename, Instant jetzt) {
        Konto konto = new Konto();
        konto.id = UUID.randomUUID();
        konto.typ = KontoTyp.ANGEHOERIGER;
        konto.email = email;
        konto.passwortHash = passwortHash;
        konto.anzeigename = anzeigename;
        konto.tarif = Tarif.KOSTENLOS;
        konto.erstelltAm = jetzt;
        return konto;
    }

    UUID getId() {
        return id;
    }

    KontoTyp getTyp() {
        return typ;
    }

    String getAnzeigename() {
        return anzeigename;
    }

    void setAnzeigename(String anzeigename) {
        this.anzeigename = anzeigename;
    }

    String getEmail() {
        return email;
    }

    String getPasswortHash() {
        return passwortHash;
    }

    Tarif getTarif() {
        return tarif;
    }

    Instant getErstelltAm() {
        return erstelltAm;
    }
}
