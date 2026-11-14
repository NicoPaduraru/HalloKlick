package de.halloklick.gemeinsam;

import java.util.Collections;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Die Sprachen, die HalloKlick anbietet. Deutsch ist die Grundsprache.
 *
 * <p>Eine neue Sprache hinzufügen:
 * <ol>
 *   <li>hier einen Eintrag ergänzen</li>
 *   <li>Server-Texte: {@code src/main/resources/sprachen/<kuerzel>.json}</li>
 *   <li>Texte der Web-App: {@code static/js/sprachen/<kuerzel>.js} und in {@code sprache.js} eintragen</li>
 *   <li>Anleitungen in dieser Sprache als neues Flyway-Skript</li>
 * </ol>
 */
public enum Sprache {
    DE("de", "german", "Deutsch"),
    EN("en", "english", "Englisch"),
    IT("it", "italian", "Italienisch");

    private final String kuerzel;
    private final String suchKonfiguration;
    private final String deutscherName;

    Sprache(String kuerzel, String suchKonfiguration, String deutscherName) {
        this.kuerzel = kuerzel;
        this.suchKonfiguration = suchKonfiguration;
        this.deutscherName = deutscherName;
    }

    /** Kürzel nach ISO 639-1, zum Beispiel "de". */
    public String kuerzel() {
        return kuerzel;
    }

    /** Name der Volltextsuche-Konfiguration in PostgreSQL (Wortstämme und Füllwörter dieser Sprache). */
    public String suchKonfiguration() {
        return suchKonfiguration;
    }

    /** So heißt die Sprache auf Deutsch. Damit sagen wir der KI, in welcher Sprache sie antworten soll. */
    public String deutscherName() {
        return deutscherName;
    }

    public Locale locale() {
        return Locale.forLanguageTag(kuerzel);
    }

    /** Unbekannte Sprachen fallen auf Deutsch zurück. */
    public static Sprache von(Locale locale) {
        if (locale != null) {
            for (Sprache sprache : values()) {
                if (sprache.kuerzel.equals(locale.getLanguage())) {
                    return sprache;
                }
            }
        }
        return DE;
    }

    public static Sprache von(String kuerzel) {
        return kuerzel == null ? DE : von(Locale.forLanguageTag(kuerzel));
    }

    /**
     * Liest die gewünschte Sprache aus der Anfrage (Kopfzeile Accept-Language).
     * Die Web-App schickt dort die Sprache mit, die der Nutzer gewählt hat.
     */
    public static Sprache von(HttpServletRequest anfrage) {
        if (anfrage.getHeader("Accept-Language") == null) {
            return DE;
        }
        for (Locale locale : Collections.list(anfrage.getLocales())) {
            for (Sprache sprache : values()) {
                if (sprache.kuerzel.equals(locale.getLanguage())) {
                    return sprache;
                }
            }
        }
        return DE;
    }
}
