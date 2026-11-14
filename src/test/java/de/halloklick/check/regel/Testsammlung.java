package de.halloklick.check.regel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Lädt die Testsammlung aus src/test/resources/testsammlung. */
public final class Testsammlung {

    private Testsammlung() {
    }

    public static List<String> betrug() {
        return lade("/testsammlung/betrug.txt");
    }

    public static List<String> harmlos() {
        return lade("/testsammlung/harmlos.txt");
    }

    /** Feste Testliste: Betrugsnachrichten, die für immer Rot bleiben müssen. */
    public static List<String> festeListeBetrug() {
        return lade("/testsammlung/feste-liste-betrug.txt");
    }

    /** Feste Testliste: harmlose Nachrichten, die nie Rot bekommen dürfen. */
    public static List<String> festeListeHarmlos() {
        return lade("/testsammlung/feste-liste-harmlos.txt");
    }

    private static List<String> lade(String pfad) {
        try (InputStream datei = Testsammlung.class.getResourceAsStream(pfad)) {
            String inhalt = new String(datei.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.stream(inhalt.split("(?m)^---\\s*$"))
                    .map(block -> block.lines().filter(zeile -> !zeile.startsWith("#")).reduce("",
                            (a, b) -> a.isEmpty() ? b : a + "\n" + b).strip())
                    .filter(block -> !block.isEmpty())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
