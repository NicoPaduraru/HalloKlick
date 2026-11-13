package de.halloklick.check.regel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.halloklick.gemeinsam.Stufe;

/**
 * Qualität messen, bevor es live geht (Bauplan, Abschnitt 7):
 * Jede Änderung an den Regeln läuft gegen die ganze Testsammlung.
 */
class TestsammlungTest {

    private final RegelPruefer pruefer = new RegelPruefer();

    @Test
    void dieSammlungHatJe50Nachrichten() {
        assertThat(Testsammlung.betrug()).hasSize(50);
        assertThat(Testsammlung.harmlos()).hasSize(50);
    }

    /** Das wichtigste Ziel: KEINE Betrugsnachricht darf Orange bekommen. */
    @Test
    void keineBetrugsnachrichtBekommtOrange() {
        List<String> durchgerutscht = Testsammlung.betrug().stream()
                .filter(nachricht -> pruefer.pruefe(nachricht).stufe() != Stufe.ROT)
                .toList();
        assertThat(durchgerutscht).as("Betrugsnachrichten, die nicht Rot bekommen haben").isEmpty();
    }

    /** Fehlalarme kosten Vertrauen. Darum darf auch keine harmlose Nachricht der Sammlung Rot bekommen. */
    @Test
    void keineHarmloseNachrichtBekommtRot() {
        List<String> fehlalarme = Testsammlung.harmlos().stream()
                .filter(nachricht -> pruefer.pruefe(nachricht).stufe() == Stufe.ROT)
                .toList();
        assertThat(fehlalarme).as("Harmlose Nachrichten, die Rot bekommen haben").isEmpty();
    }

    /** Kostenbremse: Für klar harmlose Nachrichten soll die KI möglichst selten gefragt werden. */
    @Test
    void dieMeistenHarmlosenNachrichtenBrauchenKeineKi() {
        long unklar = Testsammlung.harmlos().stream().filter(n -> pruefer.pruefe(n).unklar()).count();
        assertThat(unklar).as("harmlose Nachrichten, die zur KI gehen würden").isLessThanOrEqualTo(15);
    }
}
