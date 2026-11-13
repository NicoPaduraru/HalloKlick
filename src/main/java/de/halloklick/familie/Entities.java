package de.halloklick.familie;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Die vier kleinen Entities des Moduls stehen zusammen in dieser Datei.

@Entity
@Table(name = "familie")
class Familie {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "erstellt_von")
    private UUID erstelltVon;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected Familie() {
        // für JPA
    }

    Familie(String name, UUID erstelltVon, Instant jetzt) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.erstelltVon = erstelltVon;
        this.erstelltAm = jetzt;
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }
}

@Embeddable
class MitgliedschaftId implements Serializable {

    @Column(name = "familie_id")
    private UUID familieId;

    @Column(name = "konto_id")
    private UUID kontoId;

    protected MitgliedschaftId() {
        // für JPA
    }

    MitgliedschaftId(UUID familieId, UUID kontoId) {
        this.familieId = familieId;
        this.kontoId = kontoId;
    }

    UUID getFamilieId() {
        return familieId;
    }

    UUID getKontoId() {
        return kontoId;
    }

    @Override
    public boolean equals(Object anderes) {
        return anderes instanceof MitgliedschaftId id
                && Objects.equals(familieId, id.familieId) && Objects.equals(kontoId, id.kontoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(familieId, kontoId);
    }
}

@Entity
@Table(name = "mitgliedschaft")
class Mitgliedschaft {

    @EmbeddedId
    private MitgliedschaftId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rolle rolle;

    @Column(name = "beigetreten_am", nullable = false)
    private Instant beigetretenAm;

    protected Mitgliedschaft() {
        // für JPA
    }

    Mitgliedschaft(UUID familieId, UUID kontoId, Rolle rolle, Instant jetzt) {
        this.id = new MitgliedschaftId(familieId, kontoId);
        this.rolle = rolle;
        this.beigetretenAm = jetzt;
    }

    UUID getFamilieId() {
        return id.getFamilieId();
    }

    UUID getKontoId() {
        return id.getKontoId();
    }

    Rolle getRolle() {
        return rolle;
    }

    Instant getBeigetretenAm() {
        return beigetretenAm;
    }
}

@Entity
@Table(name = "einladung")
class Einladung {

    @Id
    private UUID id;

    @Column(name = "familie_id", nullable = false)
    private UUID familieId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "name_fuer_senior")
    private String nameFuerSenior;

    @Column(name = "gueltig_bis", nullable = false)
    private Instant gueltigBis;

    @Column(nullable = false)
    private boolean verwendet;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected Einladung() {
        // für JPA
    }

    Einladung(UUID familieId, String codeHash, String nameFuerSenior, Instant gueltigBis, Instant jetzt) {
        this.id = UUID.randomUUID();
        this.familieId = familieId;
        this.codeHash = codeHash;
        this.nameFuerSenior = nameFuerSenior;
        this.gueltigBis = gueltigBis;
        this.erstelltAm = jetzt;
    }

    UUID getId() {
        return id;
    }

    UUID getFamilieId() {
        return familieId;
    }

    String getNameFuerSenior() {
        return nameFuerSenior;
    }

    Instant getGueltigBis() {
        return gueltigBis;
    }
}

@Entity
@Table(name = "hilfeanfrage")
class Hilfeanfrage {

    @Id
    private UUID id;

    @Column(name = "von_konto_id", nullable = false)
    private UUID vonKontoId;

    @Column(name = "familie_id", nullable = false)
    private UUID familieId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HilfeThema thema;

    @Column(name = "geteilter_text")
    private String geteilterText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HilfeStatus status;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    @Column(name = "erledigt_am")
    private Instant erledigtAm;

    protected Hilfeanfrage() {
        // für JPA
    }

    Hilfeanfrage(UUID vonKontoId, UUID familieId, HilfeThema thema, String geteilterText, Instant jetzt) {
        this.id = UUID.randomUUID();
        this.vonKontoId = vonKontoId;
        this.familieId = familieId;
        this.thema = thema;
        this.geteilterText = geteilterText;
        this.status = HilfeStatus.OFFEN;
        this.erstelltAm = jetzt;
    }

    void setStatus(HilfeStatus status, Instant jetzt) {
        this.status = status;
        this.erledigtAm = status == HilfeStatus.ERLEDIGT ? jetzt : null;
    }

    UUID getId() {
        return id;
    }

    UUID getVonKontoId() {
        return vonKontoId;
    }

    UUID getFamilieId() {
        return familieId;
    }

    HilfeThema getThema() {
        return thema;
    }

    String getGeteilterText() {
        return geteilterText;
    }

    HilfeStatus getStatus() {
        return status;
    }

    Instant getErstelltAm() {
        return erstelltAm;
    }

    Instant getErledigtAm() {
        return erledigtAm;
    }
}
