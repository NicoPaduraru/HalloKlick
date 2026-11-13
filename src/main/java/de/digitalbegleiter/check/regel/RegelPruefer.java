package de.digitalbegleiter.check.regel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import de.digitalbegleiter.check.regel.LinkAnalyse.Art;
import de.digitalbegleiter.check.regel.LinkAnalyse.Link;
import de.digitalbegleiter.gemeinsam.Kategorie;
import de.digitalbegleiter.gemeinsam.Stufe;

/**
 * Stufe 1 des Sicherheitschecks: feste Regeln in Java, kostenlos und ohne KI.
 *
 * <p>Die Prüfung sucht einzelne Warnzeichen ({@link Merkmal}) und entscheidet dann
 * über Muster aus mehreren Zeichen. Ein einzelnes Wort wie "sofort" macht noch kein Rot.
 * Erst die Kombination zählt, zum Beispiel: Link UND Zeitdruck UND Bank.
 *
 * <p>Alle Wörter stehen hier ohne Umlaute (ä = ae, ß = ss), weil der Text vor der
 * Prüfung genauso umgeschrieben wird. So ist es egal, wie jemand tippt.
 */
@Component
public class RegelPruefer {

    // ---------- Warnwörter ----------

    private static final Pattern ZEITDRUCK = muster(
            "sofort", "umgehend", "dringend", "unverzueglich", "schnellstmoeglich", "so schnell wie moeglich",
            "innerhalb (von )?(\\d+|einer|zwei|drei) ?(stunden?|std|tagen?|h\\b)", "binnen \\d+",
            "in den naechsten \\d+ (stunden|tagen)", "heute noch", "noch heute", "bis heute", "bis morgen",
            "letzte (mahnung|erinnerung|chance|warnung|aufforderung|moeglichkeit)", "laeuft (heute |bald |in kuerze )?ab",
            "nur noch (heute|\\d+)", "\\beilt\\b", "\\bfrist\\b", "jetzt handeln", "rechtzeitig");

    private static final Pattern DROHUNG = muster(
            "gesperrt", "sperrung", "sperren", "eingeschraenkt", "einschraenkung", "deaktivier", "suspendiert",
            "ausgesetzt", "\\bgeloescht\\b.{0,40}(konto|account|zugang)", "(konto|account|zugang).{0,40}\\bgeloescht\\b",
            "mahnung", "mahnbescheid", "inkasso", "pfaendung", "vollstreck", "haftbefehl", "strafanzeige",
            "anzeige erstattet", "gerichtlich", "rechtliche schritte", "bussgeld", "strafverfahren",
            "(ungewoehnliche|verdaechtige|unbefugte|unberechtigte|unautorisierte) (aktivitaet|anmeldung|zugriff|"
                    + "transaktion|abbuchung|zahlung|bewegung)",
            "sicherheitsproblem", "sicherheitsgruenden", "(zurueckgesendet|zurueckgeschickt|zurueck an den absender)",
            "(konnte|kann) nicht zugestellt", "zustellung (ist )?(fehlgeschlagen|gescheitert|nicht moeglich)",
            "zustellversuch", "angehalten", "zurueckgehalten", "abgelaufen", "kuendigung", "gekuendigt",
            "wird belastet", "abgebucht");

    private static final Pattern RECHTLICHE_DROHUNG = muster(
            "haftbefehl", "pfaendung", "vollstreck", "inkasso", "strafanzeige", "mahnbescheid", "gerichtlich",
            "strafverfahren", "rechtliche schritte");

    /** Ausdrückliche Aufforderung, sich anzumelden oder Daten zu bestätigen. */
    private static final Pattern DATENABFRAGE = muster(
            "bestaetig\\w* sie (bitte )?(ihre[nrm]? )?(daten|identitaet|konto|angaben|adresse|lieferadresse|"
                    + "anschrift|zahlung|karte|bankverbindung|kontodaten|zahlungsdaten|kundendaten|e-?mail|"
                    + "telefonnummer|handynummer|zahlungsmethode|zahlungsinformation)",
            "(daten|identitaet|konto|kontodaten|adresse|lieferadresse|zahlungsdaten|zahlungsmethode|"
                    + "zahlungsinformationen|angaben|kundendaten|bankverbindung) (zu )?"
                    + "(bestaetigen|aktualisieren|verifizieren|ueberpruefen|abgleichen|vervollstaendigen|ergaenzen|"
                    + "erneuern|eingeben)",
            "aktualisier\\w* sie", "verifizier", "legitimier", "authentifizier", "identifizier\\w* sie sich",
            "melden sie sich (jetzt |bitte |hier |erneut )?(an|ein)", "loggen sie sich", "einloggen",
            "erneut anmelden", "hier anmelden", "jetzt anmelden", "zugangsdaten", "neu registrier",
            "registrierung (erneuern|laeuft|ist abgelaufen)", "(pushtan|phototan|securego|s-id-check|tan-verfahren)"
                    + ".{0,60}(erneuer|aktualisier|aktivier|laeuft|abgelaufen|bestaetig)",
            "reaktivier", "freischalten", "entsperren");

    /** Geheimnisse, nach denen keine echte Firma fragt. */
    private static final Pattern GEHEIMNIS = muster(
            "\\bpin\\b", "\\btan\\b", "\\btans\\b", "passwort", "kennwort", "kartennummer", "kreditkartennummer",
            "\\bcvv\\b", "\\bcvc\\b", "pruefziffer", "sicherheitscode", "bestaetigungscode", "verifizierungscode",
            "sms-code", "\\bcode\\b.{0,30}(per sms|aus der sms|den sie erhalten)", "ausweiskopie",
            "foto (von )?(ihrem|deinem) (ausweis|personalausweis)", "online-banking-daten", "zugangsdaten");

    private static final Pattern GEHEIMNIS_VERLANGT = muster(
            "eingeben", "eingabe", "geben sie", "gib mir", "gib uns", "mitteilen", "teilen sie", "nennen",
            "durchgeben", "senden sie", "schicken sie", "schick mir", "schick uns", "send mir", "leite\\w* .{0,20}weiter",
            "weiterleiten", "angeben", "bestaetig", "antworten sie mit", "brauche");

    /** "Geben Sie Ihre TAN niemals weiter" ist eine Warnung und keine Abfrage. */
    private static final Pattern GEHEIMNIS_WARNUNG = muster(
            "niemals", "\\bnie\\b", "nicht weiter", "an niemanden", "keinem", "kein mitarbeiter", "wir fragen nicht",
            "fragen sie nie");

    private static final Pattern GELDFORDERUNG = muster(
            "ueberweis", "zahlen sie", "bezahlen sie", "zahl\\w* (bitte|jetzt|sofort|noch)", "begleichen",
            "\\bzahlung\\b", "nachzahlung", "anzahlung", "vorkasse", "kaution", "gebuehr", "nachgebuehr", "nachporto",
            "\\bporto\\b", "versandkosten", "zollkosten", "einfuhrabgaben", "offene[rn]? (betrag|rechnung|forderung|posten)",
            "forderung", "geld (schicken|senden|leihen|vorstrecken|auslegen|ueberweisen)", "leih\\w* mir",
            "kannst du (mir )?.{0,30}(bezahlen|auslegen|vorstrecken|uebernehmen)", "rechnung (bezahlen|uebernehmen)",
            "kostenpflichtig");

    private static final Pattern BETRAG = Pattern.compile("\\d+(?:[.,]\\d+)? ?(?:€|euro\\b|eur\\b)");

    private static final Pattern ZAHLART = muster(
            "geschenkkarte", "gutscheinkarte", "guthabenkarte", "gutscheincode", "paysafe", "google[- ]play",
            "itunes", "apple[- ](karte|gutschein|geschenkkarte)", "steam[- ]?(karte|guthaben|gutschein)",
            "amazon[- ]?(gutschein|karte|geschenkkarte)", "bitcoin", "\\bbtc\\b", "kryptowaehrung", "ethereum",
            "\\busdt\\b", "western union", "moneygram", "\\bwallet\\b");

    private static final Pattern ZAHLART_VERLANGT = muster(
            "kaufen", "besorg", "\\bcodes?\\b", "rubbel", "foto (der|von der|von den) karte", "\\bzahlen\\b",
            "bezahl", "ueberweis", "einzahlen", "senden sie", "sende \\d", "transferier", "\\bwallet\\b");

    private static final Pattern FAMILIEN_ANREDE = muster(
            "\\bmama\\b", "\\bpapa\\b", "\\bmami\\b", "\\bpapi\\b", "\\bmutti\\b", "\\bvati\\b", "\\boma\\b",
            "\\bopa\\b", "\\bmutter\\b", "\\bvater\\b");

    private static final Pattern NUMMERNWECHSEL = muster(
            "neue[ns]? (handy|telefon|mobil)?nummer", "nummer (hat sich )?geaendert", "andere[nr]? nummer",
            "(diese|die neue|meine neue) nummer (ein)?speichern", "speicher\\w* (dir )?(diese|die|meine) (neue )?nummer",
            "alte (nummer )?(kannst du )?loeschen", "nummer gewechselt", "das ist (jetzt )?meine nummer",
            "unter dieser nummer");

    private static final Pattern HANDY_KAPUTT = muster(
            "handy (ist |war )?(kaputt|defekt|verloren|geklaut|gestohlen|weg)", "handy (ist )?(ins|in die|in das) "
                    + "(wasser|klo|toilette|waschmaschine)", "handy (ist )?(mir )?(runter|herunter)gefallen",
            "display (ist )?(kaputt|gebrochen|gesprungen)");

    /** "Neues Handy, aber die Nummer bleibt gleich" ist kein Nummernwechsel. */
    private static final Pattern NUMMER_BLEIBT = muster(
            "nummer bleibt", "nummer (ist|bleibt) (noch )?(die )?(gleiche?|selbe|dieselbe)",
            "(gleiche|selbe|dieselbe) nummer");

    /** Schocknachricht: Angst machen (Unfall, Festnahme) und dann Geld verlangen. */
    private static final Pattern SCHOCK = muster(
            "unfall", "verhaftet", "festgenommen", "festnahme", "\\bhaft\\b", "untersuchungshaft", "notoperation",
            "lebensgefahr", "intensivstation");

    /** Geld auf ein "sicheres Konto" bringen: Das verlangt keine echte Bank und keine Polizei. */
    private static final Pattern SICHERES_KONTO = muster(
            "sichere[sn]? konto", "sicherheitskonto", "treuhandkonto", "schutzkonto", "in sicherheit bringen");

    private static final Pattern POLIZEI = muster(
            "polizei", "europol", "interpol", "\\bbka\\b", "staatsanwalt", "kriminalbeamt");

    private static final Pattern GEWINN = muster(
            "gewonnen", "gewinner", "gewinnspiel", "hauptgewinn", "(ihr|dein|der) gewinn\\b", "gewinn in hoehe",
            "gewinnbenachrichtigung", "ausgelost", "wurden? (zufaellig )?ausgewaehlt", "erbschaft", "geerbt",
            "millionen (euro|dollar|usd)", "lotterie", "geschenk (wartet|fuer sie)", "praemie", "treuepraemie",
            "treuegeschenk", "gratis[- ](iphone|handy|geschenk)", "kostenloses? (iphone|handy|geschenk)",
            "rueckerstattung", "erstattung", "steuerrueckzahlung", "guthaben (in hoehe|von)");

    private static final Pattern FERNZUGRIFF_PROGRAMM = muster(
            "anydesk", "teamviewer", "fernwartung", "fernzugriff", "remote[- ]?(zugriff|desktop|support)",
            "\\.apk\\b", "quicksupport", "rustdesk");

    private static final Pattern INFEKTION = muster(
            "virus", "viren", "infiziert", "trojaner", "malware", "schadsoftware", "gehackt",
            "(microsoft|windows|apple)[- ]?(support|kundendienst|sicherheit|techniker)", "technischer support",
            "sicherheitswarnung");

    private static final Pattern INSTALLIEREN = muster(
            "installier", "herunterladen", "laden sie .{0,30}(herunter|app)", "download", "app (oeffnen|laden)");

    private static final Pattern SCHNELLES_GELD = muster(
            "nebenjob", "nebenverdienst", "nebeneinkommen", "heimarbeit", "von zu hause (aus )?(arbeiten|verdienen)",
            "\\d+ ?(€|euro) (pro|am|je) (tag|stunde|woche)", "(taeglich|pro tag|woechentlich) .{0,20}verdienen",
            "geld verdienen", "rendite", "garantiert\\w* (gewinn|rendite|ertrag)", "investier", "investition",
            "investment", "trading", "passives einkommen", "schnell(es)? geld", "job[- ]?angebot", "stellenangebot",
            "teilzeitjob", "personalabteilung", "recruiter", "finanzielle freiheit", "krypto");

    private static final Pattern KONTAKT_MESSENGER = muster("whatsapp", "telegram", "kontaktieren sie", "schreiben sie "
            + "(uns|mir)", "melden sie sich", "antworten sie");

    private static final Pattern ERPRESSUNG = muster(
            "webcam", "kamera .{0,30}(gehackt|aufgenommen|zugriff)", "aufgenommen", "aufnahmen von (ihnen|dir)",
            "\\bintime", "pornogra", "erwachsenen-?(webseite|seite|inhalte)", "ihr passwort (kenne|habe|lautet)",
            "ihr geraet .{0,30}(gehackt|zugriff)", "video von (ihnen|dir)", "veroeffentlich");

    private static final Pattern TELEFONNUMMER = Pattern.compile("(?:\\+ ?49|0049|\\b0)[ /-]?[1-9](?:[ /-]?\\d){6,}");

    private static final Pattern ANRUF_VERLANGT = muster(
            "rufen sie", "ruf (mich |uns )?(bitte |sofort |schnell |dringend )?(an|zurueck)", "anrufen", "rueckruf",
            "melden sie sich", "kontaktieren sie", "hotline", "erreichen sie uns", "telefonisch");

    // ---------- Wörter für die Kategorie ----------

    private static final Pattern BANK = muster(
            "\\bbank\\b", "\\w+bank\\b", "sparkasse", "raiffeisen", "bankkonto", "girokonto", "online-?banking",
            "\\bbanking\\b", "\\btans?\\b", "pushtan", "phototan", "chiptan", "securego", "s-id-check",
            "kreditkarte", "\\bvisa\\b", "mastercard", "paypal", "klarna", "comdirect", "\\bing\\b", "\\bdkb\\b",
            "\\bn26\\b", "lastschrift", "\\biban\\b", "ec-karte", "girocard", "bankkarte", "debitkarte",
            "kontoinhaber", "kontostand", "dispo");

    private static final Pattern PAKET = muster(
            "paket", "sendung", "zustell", "liefer", "\\bzoll", "\\bdhl\\b", "hermes", "\\bdpd\\b", "\\bups\\b",
            "\\bgls\\b", "fedex", "deutsche post", "packstation", "paketshop", "versand", "bestellung");

    private static final Pattern BEHOERDE = muster(
            "finanzamt", "steuer", "elster", "gericht", "polizei", "staatsanwalt", "bundes\\w+", "behoerde",
            "rentenversicherung", "krankenkasse", "europol", "interpol", "bussgeld", "vollstreck", "inkasso",
            "rundfunkbeitrag", "beitragsservice", "mahnbescheid", "ordnungsamt", "buergeramt", "haftbefehl",
            "kriminalpolizei", "\\bbka\\b", "\\bbzst\\b", "anwalt", "kanzlei");

    private static final Pattern KUNDENKONTO = muster(
            "amazon", "netflix", "apple[- ]id", "icloud", "microsoft[- ]konto", "google[- ]konto", "ebay",
            "kleinanzeigen", "telekom", "vodafone", "\\bo2\\b", "1&1", "disney", "spotify", "\\babo\\b",
            "abonnement", "mitgliedschaft", "kundenkonto", "benutzerkonto", "nutzerkonto", "(ihr|dein|ihrem|deinem|"
                    + "ihres|deines) (konto|account|profil)", "\\baccount\\b", "facebook", "instagram", "\\bprime\\b",
            "paketkonto", "speicherplatz", "postfach");

    private static final Set<Kategorie> ERNSTE_KATEGORIEN =
            EnumSet.of(Kategorie.BANK, Kategorie.PAKET, Kategorie.BEHOERDE, Kategorie.KUNDENKONTO);

    // ---------- Prüfung ----------

    public RegelErgebnis pruefe(String text) {
        String klein = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.GERMAN)
                // unsichtbare Zeichen, mit denen Betrüger Wörter vor Filtern verstecken
                .replaceAll("[\\u200B-\\u200D\\uFEFF\\u00AD]", "");
        List<Link> links = LinkAnalyse.finde(klein);
        String t = klein.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
                .replaceAll("\\s+", " ");

        EnumSet<Merkmal> merkmale = EnumSet.noneOf(Merkmal.class);

        // 1. Links
        boolean fremderLink = false;
        for (Link link : links) {
            switch (link.art()) {
                case NACHGEMACHT -> merkmale.add(Merkmal.NACHGEMACHTE_ADRESSE);
                case IP -> merkmale.add(Merkmal.IP_LINK);
                case KURZLINK -> merkmale.add(Merkmal.KURZLINK);
                case UNGEWOEHNLICH -> merkmale.add(Merkmal.UNGEWOEHNLICHE_ADRESSE);
                default -> { }
            }
            // Ein Link zur echten Adresse einer bekannten Firma zählt nicht als Warnzeichen
            fremderLink |= link.art() != Art.OFFIZIELL;
        }
        if (fremderLink) {
            merkmale.add(Merkmal.LINK);
        }

        // 2. Einzelne Warnzeichen
        boolean zeitdruck = merke(merkmale, Merkmal.ZEITDRUCK, findet(ZEITDRUCK, t));
        boolean drohung = merke(merkmale, Merkmal.DROHUNG, findet(DROHUNG, t));
        boolean datenabfrage = merke(merkmale, Merkmal.DATENABFRAGE, findet(DATENABFRAGE, t));
        boolean geheimnis = merke(merkmale, Merkmal.GEHEIME_DATEN,
                findet(GEHEIMNIS, t) && findet(GEHEIMNIS_VERLANGT, t) && !findet(GEHEIMNIS_WARNUNG, t));
        boolean geld = merke(merkmale, Merkmal.GELDFORDERUNG, findet(GELDFORDERUNG, t));
        boolean betrag = BETRAG.matcher(t).find();
        boolean zahlart = merke(merkmale, Merkmal.UNGEWOEHNLICHE_ZAHLART,
                findet(ZAHLART, t) && findet(ZAHLART_VERLANGT, t));
        boolean familie = findet(FAMILIEN_ANREDE, t);
        boolean handyKaputt = findet(HANDY_KAPUTT, t);
        boolean neueNummer = merke(merkmale, Merkmal.NEUE_NUMMER,
                familie && !findet(NUMMER_BLEIBT, t)
                        && (findet(NUMMERNWECHSEL, t) || (handyKaputt && t.contains("nummer"))));
        boolean gewinn = merke(merkmale, Merkmal.GEWINN, findet(GEWINN, t));
        boolean rueckruf = merke(merkmale, Merkmal.RUECKRUF,
                TELEFONNUMMER.matcher(t).find() && findet(ANRUF_VERLANGT, t));
        boolean infektion = findet(INFEKTION, t);
        boolean fernzugriff = merke(merkmale, Merkmal.FERNZUGRIFF, findet(FERNZUGRIFF_PROGRAMM, t)
                || (infektion && (rueckruf || fremderLink || findet(INSTALLIEREN, t))));
        boolean schnellesGeld = merke(merkmale, Merkmal.SCHNELLES_GELD, findet(SCHNELLES_GELD, t));
        boolean schock = merke(merkmale, Merkmal.SCHOCK, findet(SCHOCK, t) && (geld || betrag));
        boolean erpressung = merke(merkmale, Merkmal.ERPRESSUNG,
                findet(ERPRESSUNG, t) && (geld || betrag || findet(ZAHLART, t)));

        // 3. Kategorie
        Kategorie kategorie = bestimmeKategorie(t, neueNummer, schock || (familie && (geld || handyKaputt)), gewinn,
                schnellesGeld, fernzugriff || erpressung);
        boolean ernst = ERNSTE_KATEGORIEN.contains(kategorie);
        boolean druck = zeitdruck || drohung;

        // 4. Muster: Erst die Kombination ergibt Rot
        boolean rot =
                // Nachgemachte Adresse oder Adresse aus Zahlen: immer Rot
                merkmale.contains(Merkmal.NACHGEMACHTE_ADRESSE) || merkmale.contains(Merkmal.IP_LINK)
                // Kurzlink oder ungewöhnliche Adresse zusammen mit irgendeinem anderen Zeichen
                || ((merkmale.contains(Merkmal.KURZLINK) || merkmale.contains(Merkmal.UNGEWOEHNLICHE_ADRESSE))
                        && (druck || datenabfrage || geld || betrag || gewinn || kategorie != Kategorie.SONSTIGES))
                // Bauplan: Link UND Zeitdruck UND Bank/Paket/Behörde ergibt immer Rot
                || (fremderLink && druck && ernst)
                || (fremderLink && datenabfrage)
                || (fremderLink && (geld || betrag) && ernst)
                // Nach PIN, TAN oder Passwort fragt keine echte Firma
                || geheimnis
                || (datenabfrage && druck && ernst)
                // Enkeltrick per Nachricht
                || neueNummer
                || (familie && handyKaputt && (geld || betrag))
                || (familie && geld && zeitdruck)
                || zahlart
                || (gewinn && (fremderLink || datenabfrage || geld || rueckruf))
                || fernzugriff
                || erpressung
                || schock
                // Geld auf ein "sicheres Konto" oder Geld an die "Polizei": immer Betrug
                || findet(SICHERES_KONTO, t)
                || (findet(POLIZEI, t) && geld)
                || (schnellesGeld && (fremderLink || findet(KONTAKT_MESSENGER, t) || rueckruf))
                || (rueckruf && druck && (ernst || kategorie == Kategorie.TECHNIK))
                || (findet(RECHTLICHE_DROHUNG, t) && (geld || betrag) && (fremderLink || rueckruf || zeitdruck));

        Stufe stufe = rot ? Stufe.ROT : Stufe.ORANGE;
        // Unklar: Es gibt Warnzeichen, aber kein eindeutiges Muster. Dann lohnt sich die KI.
        boolean unklar = !rot && !merkmale.isEmpty();
        return new RegelErgebnis(stufe, kategorie, new ArrayList<>(merkmale), unklar);
    }

    private static Kategorie bestimmeKategorie(String t, boolean neueNummer, boolean familieMitGeld,
            boolean gewinn, boolean schnellesGeld, boolean technik) {
        if (neueNummer) {
            return Kategorie.FAMILIE;
        }
        long bank = zaehle(BANK, t);
        long paket = zaehle(PAKET, t);
        long behoerde = zaehle(BEHOERDE, t);
        long kundenkonto = zaehle(KUNDENKONTO, t);
        long hoechste = Math.max(Math.max(bank, paket), Math.max(behoerde, kundenkonto));
        if (hoechste > 0) {
            // Bei Gleichstand gewinnt die Reihenfolge Bank, Paket, Behörde, Kundenkonto
            if (bank == hoechste) {
                return Kategorie.BANK;
            }
            if (paket == hoechste) {
                return Kategorie.PAKET;
            }
            return behoerde == hoechste ? Kategorie.BEHOERDE : Kategorie.KUNDENKONTO;
        }
        if (familieMitGeld) {
            return Kategorie.FAMILIE;
        }
        if (gewinn) {
            return Kategorie.GEWINN;
        }
        if (schnellesGeld) {
            return Kategorie.GELDANLAGE;
        }
        return technik ? Kategorie.TECHNIK : Kategorie.SONSTIGES;
    }

    private static Pattern muster(String... teile) {
        return Pattern.compile("(?:" + String.join("|", teile) + ")");
    }

    private static boolean findet(Pattern muster, String text) {
        return muster.matcher(text).find();
    }

    private static long zaehle(Pattern muster, String text) {
        return muster.matcher(text).results().count();
    }

    private static boolean merke(Set<Merkmal> merkmale, Merkmal merkmal, boolean gefunden) {
        if (gefunden) {
            merkmale.add(merkmal);
        }
        return gefunden;
    }
}
