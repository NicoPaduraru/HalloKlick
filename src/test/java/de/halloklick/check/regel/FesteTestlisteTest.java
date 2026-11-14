package de.halloklick.check.regel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.halloklick.gemeinsam.Stufe;

/**
 * Die feste Testliste: Was hier einmal steht, darf nie wieder kaputtgehen.
 * Jede Nachricht ist ein eigener Test. So sieht man sofort, WELCHE Nachricht durchgerutscht ist.
 *
 * <p>Neue Nachrichten kommen in die Dateien unter src/test/resources/testsammlung/feste-liste-*.txt.
 */
class FesteTestlisteTest {

    private final RegelPruefer pruefer = new RegelPruefer();

    static Stream<String> betrug() {
        return Testsammlung.festeListeBetrug().stream();
    }

    static Stream<String> harmlos() {
        return Testsammlung.festeListeHarmlos().stream();
    }

    @ParameterizedTest(name = "Betrug bleibt Rot: {0}")
    @MethodSource("betrug")
    void betrugBleibtRot(String nachricht) {
        assertThat(pruefer.pruefe(nachricht).stufe()).isEqualTo(Stufe.ROT);
    }

    @ParameterizedTest(name = "Harmlos wird nie Rot: {0}")
    @MethodSource("harmlos")
    void harmlosWirdNieRot(String nachricht) {
        RegelErgebnis ergebnis = pruefer.pruefe(nachricht);
        assertThat(ergebnis.stufe()).as("Merkmale: " + ergebnis.merkmale()).isEqualTo(Stufe.ORANGE);
    }

    @Test
    void dieListenSindNichtLeer() {
        assertThat(Testsammlung.festeListeBetrug()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(Testsammlung.festeListeHarmlos()).hasSizeGreaterThanOrEqualTo(14);
    }

    // ---------- Die neuen Regeln einzeln ----------

    @Test
    void codeWeiterleitenIstRot() {
        RegelErgebnis ergebnis = pruefer.pruefe("Leite mir bitte den Code weiter, den du gerade bekommen hast.");
        assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
        assertThat(ergebnis.merkmale()).contains(Merkmal.GEHEIME_DATEN);
    }

    @Test
    void falschePolizeiMitBargeldUndSchmuckIstRot() {
        RegelErgebnis ergebnis = pruefer.pruefe("Hier ist die Polizei. Bitte legen Sie Bargeld und Schmuck bereit.");
        assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
        assertThat(ergebnis.merkmale()).contains(Merkmal.FALSCHE_POLIZEI);
    }

    @ParameterizedTest
    @ValueSource(strings = {"netflx.com", "paypall.de", "amazn.de", "sparkase.de", "netfilx.com", "micros0ft.com",
            "vodafne.de", "telekon.de"})
    void tippfehlerAdressenWerdenErkannt(String adresse) {
        RegelErgebnis ergebnis = pruefer.pruefe("Bitte ansehen: https://" + adresse + "/start");
        assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
        assertThat(ergebnis.merkmale()).contains(Merkmal.NACHGEMACHTE_ADRESSE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"netflix.com", "telecom.de", "paypal.com", "www.amazon.de", "apply-now.de",
            "klara-blumen.de", "hermann.de"})
    void echteOderAndereAdressenSindKeinTippfehler(String adresse) {
        assertThat(pruefer.pruefe("Bitte ansehen: https://" + adresse).merkmale())
                .doesNotContain(Merkmal.NACHGEMACHTE_ADRESSE);
    }

    @Test
    void einTippfehlerIstGenauEinFehler() {
        assertThat(LinkAnalyse.istTippfehlerVon("netflx", "netflix")).isTrue();     // fehlt
        assertThat(LinkAnalyse.istTippfehlerVon("netfflix", "netflix")).isTrue();   // zu viel
        assertThat(LinkAnalyse.istTippfehlerVon("netflex", "netflix")).isTrue();    // falsch
        assertThat(LinkAnalyse.istTippfehlerVon("netfilx", "netflix")).isTrue();    // vertauscht
        assertThat(LinkAnalyse.istTippfehlerVon("netflix", "netflix")).isFalse();   // richtig geschrieben
        assertThat(LinkAnalyse.istTippfehlerVon("nettflixx", "netflix")).isFalse(); // zwei Fehler
        assertThat(LinkAnalyse.istTippfehlerVon("wetter", "netflix")).isFalse();
    }
}
