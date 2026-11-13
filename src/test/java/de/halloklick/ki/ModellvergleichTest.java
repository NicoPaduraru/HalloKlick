package de.halloklick.ki;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import de.halloklick.check.regel.Testsammlung;
import de.halloklick.gemeinsam.Stufe;

/**
 * Modellvergleich (Bauplan, Schritt 4.6): Opus 5.5 und Haiku 5.5 gegen die Testsammlung.
 *
 * <p>ACHTUNG: Dieser Test ruft die echte Claude API auf und kostet Geld (200 Aufrufe).
 * Er läuft darum nur, wenn du ihn ausdrücklich startest:
 *
 * <pre>
 *   Windows (PowerShell):
 *     $env:MODELLVERGLEICH = "ja"; $env:ANTHROPIC_API_KEY = "..."; .\mvnw.cmd test "-Dtest=ModellvergleichTest"
 * </pre>
 *
 * Hier wird nur die KI allein gemessen, ohne die Regeln. Das günstigere Modell kommt in Frage,
 * wenn es KEINE Betrugsnachricht durchlässt. Die Entscheidung triffst du.
 */
@EnabledIfEnvironmentVariable(named = "MODELLVERGLEICH", matches = "ja")
class ModellvergleichTest {

    private record Modell(String id, String preisEingabe, String preisAusgabe) {
    }

    /** Preise in US-Dollar pro 1 Million Tokens. Vor dem Vergleich bei Anthropic prüfen. */
    private static final List<Modell> MODELLE = List.of(
            new Modell("claude-opus-5-5", "4.00", "20.00"),
            new Modell("claude-haiku-5-5", "0.10", "0.50"));

    @Test
    void vergleicheModelle() {
        String schluessel = System.getenv("ANTHROPIC_API_KEY");
        for (Modell modell : MODELLE) {
            ClaudeNachrichtenPruefer pruefer = new ClaudeNachrichtenPruefer(schluessel, modell.id());
            long eingabe = 0;
            long ausgabe = 0;
            int fehler = 0;
            List<String> durchgelassen = new ArrayList<>();
            List<String> fehlalarme = new ArrayList<>();

            for (String nachricht : Testsammlung.betrug()) {
                try {
                    KiEinschaetzung e = pruefer.pruefe(PruefEingabe.ausText(nachricht));
                    eingabe += e.eingabeTokens();
                    ausgabe += e.ausgabeTokens();
                    if (e.stufe() != Stufe.ROT) {
                        durchgelassen.add(nachricht);
                    }
                } catch (KiNichtVerfuegbarException e) {
                    fehler++;
                }
            }
            for (String nachricht : Testsammlung.harmlos()) {
                try {
                    KiEinschaetzung e = pruefer.pruefe(PruefEingabe.ausText(nachricht));
                    eingabe += e.eingabeTokens();
                    ausgabe += e.ausgabeTokens();
                    if (e.stufe() == Stufe.ROT) {
                        fehlalarme.add(nachricht);
                    }
                } catch (KiNichtVerfuegbarException e) {
                    fehler++;
                }
            }

            BigDecimal kosten = new BigDecimal(modell.preisEingabe()).multiply(BigDecimal.valueOf(eingabe))
                    .add(new BigDecimal(modell.preisAusgabe()).multiply(BigDecimal.valueOf(ausgabe)))
                    .divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP);

            System.out.println();
            System.out.println("=== " + modell.id() + " ===");
            System.out.println("Betrug durchgelassen (Orange statt Rot): " + durchgelassen.size() + " von 50");
            durchgelassen.forEach(n -> System.out.println("   - " + n.replace('\n', ' ')));
            System.out.println("Fehlalarme (harmlos, aber Rot):          " + fehlalarme.size() + " von 50");
            fehlalarme.forEach(n -> System.out.println("   - " + n.replace('\n', ' ')));
            System.out.println("Ausfälle oder Ablehnungen:               " + fehler);
            System.out.println("Tokens: " + eingabe + " Eingabe, " + ausgabe + " Ausgabe");
            System.out.println("Kosten für 100 Nachrichten: " + kosten + " $ (pro Nachricht etwa "
                    + kosten.divide(BigDecimal.valueOf(100), 5, RoundingMode.HALF_UP) + " $)");
        }
    }
}
