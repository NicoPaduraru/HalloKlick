package de.halloklick.check;

import java.util.regex.Pattern;

import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Stufe;
import de.halloklick.ki.KiErgebnis;
import de.halloklick.ki.NaechsterSchritt;

/**
 * Feste Texte für das Ergebnis, in einfacher Sprache und mit "Sie".
 * Alles, was den Senior zu einer Handlung auffordert, steht hier fest im Code
 * und kommt nie frei formuliert aus der KI.
 */
final class Textbausteine {

    /** Steht unter jedem Ergebnis (Bauplan, Abschnitt 7). */
    static final String HINWEIS = "Das ist eine Einschätzung, keine Garantie. Klicken Sie nicht auf Links. "
            + "Rufen Sie im Zweifel die Nummer an, die auf Ihrer Bankkarte steht.";

    static final String FOTO_NICHT_GEPRUEFT_TITEL = "Wir konnten das Foto gerade nicht prüfen.";
    static final String FOTO_NICHT_GEPRUEFT_ERKLAERUNG = "Das Foto wurde nicht geprüft. Wir wissen also nicht, "
            + "ob die Nachricht echt ist. Sie können den Text der Nachricht abtippen oder einfügen. "
            + "Dann prüfen wir ihn.";

    private static final Pattern LINK_ODER_NUMMER = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9-]+\\.(de|com|net|org|info|ru|io|ly|eu|at|ch|xyz|top|app)\\b|\\d[\\d /-]{4,}\\d)");

    private Textbausteine() {
    }

    static String titel(Stufe stufe) {
        return stufe == Stufe.ROT
                ? "Vorsicht! Das sieht nach Betrug aus."
                : "Wir haben keine typischen Warnzeichen gefunden.";
    }

    static String erklaerung(Stufe stufe, boolean mitAuffaelligkeiten) {
        if (stufe == Stufe.ROT) {
            return "Diese Nachricht hat typische Zeichen von Betrug. Antworten Sie nicht und geben Sie keine Daten ein.";
        }
        if (mitAuffaelligkeiten) {
            return "Uns ist etwas aufgefallen, aber es ist kein eindeutiges Zeichen für Betrug. "
                    + "Bleiben Sie bitte vorsichtig.";
        }
        return "In dieser Nachricht haben wir keine typischen Warnzeichen gefunden. "
                + "Das heißt nicht, dass die Nachricht sicher ist.";
    }

    static String naechsterSchritt(Stufe stufe, Kategorie kategorie) {
        if (stufe == Stufe.ORANGE) {
            return "Sind Sie unsicher? Dann fragen Sie Ihre Familie, bevor Sie antworten oder etwas anklicken.";
        }
        return switch (kategorie) {
            case BANK -> "Klicken Sie nicht auf den Link. Rufen Sie Ihre Bank an. Nehmen Sie die Nummer, "
                    + "die auf Ihrer Bankkarte steht.";
            case PAKET -> "Klicken Sie nicht auf den Link und zahlen Sie nichts. Erwarten Sie ein Paket? "
                    + "Dann schauen Sie nur in der App oder auf der echten Internetseite des Paketdienstes nach.";
            case BEHOERDE -> "Zahlen Sie nichts und klicken Sie nicht auf den Link. Behörden schreiben Ihnen "
                    + "einen Brief. Rufen Sie im Zweifel die Behörde an. Nehmen Sie eine Nummer aus dem Telefonbuch.";
            case FAMILIE -> "Überweisen Sie kein Geld. Rufen Sie Ihr Familienmitglied unter der alten Nummer an, "
                    + "die Sie schon lange kennen.";
            case GEWINN -> "Antworten Sie nicht und zahlen Sie nichts. Für einen echten Gewinn muss man nie "
                    + "vorher Geld bezahlen.";
            case KUNDENKONTO -> "Klicken Sie nicht auf den Link. Öffnen Sie die App oder die Internetseite der "
                    + "Firma selbst und schauen Sie dort nach.";
            case GELDANLAGE -> "Antworten Sie nicht und überweisen Sie kein Geld. Sprechen Sie zuerst mit "
                    + "Ihrer Familie oder Ihrer Bank.";
            case TECHNIK -> "Installieren Sie nichts und rufen Sie die Nummer nicht an. Fragen Sie Ihre Familie.";
            case SONSTIGES -> "Antworten Sie nicht und klicken Sie nichts an. Fragen Sie Ihre Familie.";
        };
    }

    static String naechsterSchritt(NaechsterSchritt schritt) {
        return switch (schritt) {
            case NICHT_REAGIEREN -> "Antworten Sie nicht und klicken Sie nichts an. Löschen Sie die Nachricht.";
            case BANK_UEBER_BEKANNTE_NUMMER_ANRUFEN -> "Klicken Sie nicht auf den Link. Rufen Sie Ihre Bank an. "
                    + "Nehmen Sie die Nummer, die auf Ihrer Bankkarte steht.";
            case ABSENDER_UEBER_BEKANNTE_NUMMER_ANRUFEN -> "Rufen Sie den Absender an. Nehmen Sie eine Nummer, "
                    + "die Sie schon lange kennen, und nicht die Nummer aus der Nachricht.";
            case NUR_IN_OFFIZIELLER_APP_NACHSEHEN -> "Klicken Sie nicht auf den Link. Öffnen Sie die App oder die "
                    + "Internetseite der Firma selbst und schauen Sie dort nach.";
            case FAMILIE_FRAGEN -> "Fragen Sie Ihre Familie, bevor Sie antworten oder etwas anklicken.";
        };
    }

    static String kiNichtVerfuegbar(KiErgebnis.Grund grund) {
        return switch (grund) {
            case TAGESLIMIT -> "Sie haben heute schon viele Nachrichten geprüft. Darum war diese Prüfung "
                    + "einfacher als sonst. Bitte fragen Sie im Zweifel Ihre Familie.";
            default -> "Diese Prüfung war einfacher als sonst. Bitte fragen Sie im Zweifel Ihre Familie.";
        };
    }

    /**
     * Text aus der KI entschärfen: Enthält er einen Link oder eine längere Nummer, wird er
     * ganz verworfen (null). So kann eine manipulierte Nachricht über die KI keine
     * Adresse oder Telefonnummer in unser Ergebnis schmuggeln.
     */
    static String entschaerfe(String kiText, int hoechstLaenge) {
        if (kiText == null) {
            return null;
        }
        String text = kiText.replaceAll("[\\p{Cntrl}<>]", " ").replaceAll("\\s+", " ").strip();
        if (text.isEmpty() || LINK_ODER_NUMMER.matcher(text).find()) {
            return null;
        }
        return text.length() <= hoechstLaenge ? text : text.substring(0, hoechstLaenge - 1).strip() + "…";
    }
}
