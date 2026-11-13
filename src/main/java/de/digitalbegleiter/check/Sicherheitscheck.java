package de.digitalbegleiter.check;

import java.time.Instant;
import java.util.UUID;

import de.digitalbegleiter.gemeinsam.Kategorie;
import de.digitalbegleiter.gemeinsam.Stufe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Was von einem Check übrig bleibt: nur Stufe und Kategorie.
 * Der Inhalt der Nachricht wird NIE gespeichert, darum gibt es hier auch kein Feld dafür.
 */
@Entity
@Table(name = "sicherheitscheck")
class Sicherheitscheck {

    enum Quelle {
        TEXT,
        BILD
    }

    @Id
    private UUID id;

    @Column(name = "konto_id", nullable = false)
    private UUID kontoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Quelle quelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stufe stufe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kategorie kategorie;

    @Column(name = "ki_verwendet", nullable = false)
    private boolean kiVerwendet;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected Sicherheitscheck() {
        // für JPA
    }

    Sicherheitscheck(UUID kontoId, Quelle quelle, Stufe stufe, Kategorie kategorie, boolean kiVerwendet,
            Instant jetzt) {
        this.id = UUID.randomUUID();
        this.kontoId = kontoId;
        this.quelle = quelle;
        this.stufe = stufe;
        this.kategorie = kategorie;
        this.kiVerwendet = kiVerwendet;
        this.erstelltAm = jetzt;
    }

    UUID getId() {
        return id;
    }

    UUID getKontoId() {
        return kontoId;
    }

    Quelle getQuelle() {
        return quelle;
    }

    Stufe getStufe() {
        return stufe;
    }

    Kategorie getKategorie() {
        return kategorie;
    }

    boolean isKiVerwendet() {
        return kiVerwendet;
    }

    Instant getErstelltAm() {
        return erstelltAm;
    }
}
