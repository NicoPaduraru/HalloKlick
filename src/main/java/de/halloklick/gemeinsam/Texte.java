package de.halloklick.gemeinsam;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Übersetzt die Texte, die der Server an Menschen schickt.
 *
 * <p>Im Code stehen die Sätze auf Deutsch. Der deutsche Satz ist zugleich der Schlüssel,
 * unter dem die Übersetzung in {@code sprachen/en.json}, {@code sprachen/it.json} usw. steht.
 * Fehlt eine Übersetzung, kommt der deutsche Satz zurück.
 */
@Component
public class Texte {

    private final Map<Sprache, Map<String, String>> uebersetzungen = new EnumMap<>(Sprache.class);

    public Texte() {
        ObjectMapper json = new ObjectMapper();
        for (Sprache sprache : Sprache.values()) {
            if (sprache == Sprache.DE) {
                continue;
            }
            String pfad = "/sprachen/" + sprache.kuerzel() + ".json";
            try (InputStream datei = Texte.class.getResourceAsStream(pfad)) {
                if (datei == null) {
                    throw new IllegalStateException("Übersetzungsdatei fehlt: " + pfad);
                }
                uebersetzungen.put(sprache, json.readValue(datei, new TypeReference<Map<String, String>>() { }));
            } catch (IOException e) {
                throw new UncheckedIOException("Übersetzungsdatei nicht lesbar: " + pfad, e);
            }
        }
    }

    /** Übersetzt in die Sprache der laufenden Anfrage. */
    public String hier(String deutsch) {
        return in(LocaleContextHolder.getLocale(), deutsch);
    }

    public String in(Locale locale, String deutsch) {
        return in(Sprache.von(locale), deutsch);
    }

    public String in(Sprache sprache, String deutsch) {
        if (deutsch == null || sprache == Sprache.DE) {
            return deutsch;
        }
        return uebersetzungen.get(sprache).getOrDefault(deutsch, deutsch);
    }

    /** Für Tests: alle deutschen Sätze, die eine Sprache übersetzt. */
    public Map<String, String> alle(Sprache sprache) {
        return Map.copyOf(uebersetzungen.getOrDefault(sprache, Map.of()));
    }
}
