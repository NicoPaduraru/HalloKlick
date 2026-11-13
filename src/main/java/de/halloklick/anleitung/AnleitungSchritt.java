package de.halloklick.anleitung;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class AnleitungSchritt {

    @Column(nullable = false)
    private int nummer;

    @Column(nullable = false)
    private String text;

    @Column(name = "bild_pfad")
    private String bildPfad;

    protected AnleitungSchritt() {
        // für JPA
    }

    AnleitungSchritt(int nummer, String text, String bildPfad) {
        this.nummer = nummer;
        this.text = text;
        this.bildPfad = bildPfad;
    }

    int getNummer() {
        return nummer;
    }

    String getText() {
        return text;
    }

    String getBildPfad() {
        return bildPfad;
    }
}
