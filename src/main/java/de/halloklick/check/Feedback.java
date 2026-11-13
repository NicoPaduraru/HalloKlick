package de.halloklick.check;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** "War das hilfreich?" Damit messen wir die Qualität der Checks. */
@Entity
@Table(name = "feedback")
class Feedback {

    @Id
    @Column(name = "check_id")
    private UUID checkId;

    @Column(nullable = false)
    private boolean hilfreich;

    @Column(name = "erstellt_am", nullable = false)
    private Instant erstelltAm;

    protected Feedback() {
        // für JPA
    }

    Feedback(UUID checkId, boolean hilfreich, Instant jetzt) {
        this.checkId = checkId;
        this.hilfreich = hilfreich;
        this.erstelltAm = jetzt;
    }

    void setHilfreich(boolean hilfreich) {
        this.hilfreich = hilfreich;
    }

    boolean isHilfreich() {
        return hilfreich;
    }
}
