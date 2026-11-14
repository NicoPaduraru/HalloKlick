package de.halloklick.anleitung;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "anleitung")
class Anleitung {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String titel;

    @Column(nullable = false)
    private String kategorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnleitungPlattform plattform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnleitungQuelle quelle;

    @Column(name = "familie_id")
    private UUID familieId;

    @Column(nullable = false)
    private String stichworte;

    @Column(nullable = false)
    private String sprache = "de";

    @Column(name = "geprueft_am")
    private LocalDate geprueftAm;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "anleitung_schritt", joinColumns = @JoinColumn(name = "anleitung_id"))
    @OrderBy("nummer")
    private List<AnleitungSchritt> schritte = new ArrayList<>();

    protected Anleitung() {
        // für JPA
    }

    /** Eigene Anleitung einer Familie. */
    static Anleitung fuerFamilie(UUID familieId, String sprache, Instant jetzt) {
        Anleitung anleitung = new Anleitung();
        anleitung.id = UUID.randomUUID();
        anleitung.quelle = AnleitungQuelle.FAMILIE;
        anleitung.familieId = familieId;
        anleitung.sprache = sprache;
        anleitung.stichworte = "";
        anleitung.erstelltAm = jetzt;
        return anleitung;
    }

    void aendere(String titel, String kategorie, AnleitungPlattform plattform, List<String> schrittTexte) {
        this.titel = titel.strip();
        this.kategorie = kategorie.strip();
        this.plattform = plattform;
        this.schritte.clear();
        int nummer = 1;
        for (String text : schrittTexte) {
            this.schritte.add(new AnleitungSchritt(nummer++, text.strip(), null));
        }
    }

    UUID getId() {
        return id;
    }

    String getTitel() {
        return titel;
    }

    String getKategorie() {
        return kategorie;
    }

    AnleitungPlattform getPlattform() {
        return plattform;
    }

    AnleitungQuelle getQuelle() {
        return quelle;
    }

    UUID getFamilieId() {
        return familieId;
    }

    LocalDate getGeprueftAm() {
        return geprueftAm;
    }

    List<AnleitungSchritt> getSchritte() {
        return schritte;
    }
}
