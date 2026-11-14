package de.halloklick.check.regel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import de.halloklick.check.regel.LinkAnalyse.Art;
import de.halloklick.check.regel.LinkAnalyse.Link;
import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Stufe;

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

    // ---------- Codes weiterleiten ----------

    /**
     * "Ich habe dir aus Versehen einen Code geschickt, leite ihn mir bitte weiter":
     * Mit so einem Code übernehmen Betrüger ein WhatsApp- oder Bankkonto.
     * Wichtig ist die Richtung: Der Code soll ZU jemandem geschickt werden.
     */
    private static final Pattern CODE_WEITERGABE = muster(
            "(schick|sende?|sag|nenn|gib|leite|teil|verrat|brauch)\\w* (mir|uns|ihn mir|den mir|bitte|kurz|schnell|mal)"
                    + ".{0,50}\\b(code|zahlencode|ziffern|pin|tan)\\b",
            "\\b(code|zahlencode|ziffern)\\b.{0,60}(weiterleite|weiter leite|leite\\w* .{0,25}weiter|zurueckschick|"
                    + "zurueck schick|schick\\w* .{0,25}zurueck|an mich|mir (schicken|senden|sagen|nennen|geben|"
                    + "mitteilen|durchgeben|weiterleiten)|durchgeben|vorlesen)",
            "\\b(code|zahlencode)\\b.{0,80}(schick|sende?|sag|nenn|gib|leite|teil)\\w* (ihn |den |sie |es )?(mir|uns)\\b",
            "(aus versehen|versehentlich|irrtuemlich|falsche nummer).{0,80}\\b(code|sms)\\b",
            "\\b(code|sms)\\b.{0,80}(aus versehen|versehentlich|irrtuemlich)",
            "(send|forward|give|tell|share|text)\\w* (me|us|it to me|it back) .{0,40}\\b(code|pin|otp)\\b",
            "\\b(code|otp)\\b.{0,60}(forward it|send it (to me|back)|to me|by mistake|by accident)",
            "(by mistake|by accident|accidentally).{0,80}\\b(code|otp)\\b");

    /** Der Hinweis der echten Firma, den Code niemandem zu geben, ist keine Abfrage. */
    private static final Pattern CODE_WARNUNG = muster(
            "niemals", "\\bnie\\b", "nicht weiter", "an niemanden", "keinem", "\\bnever\\b", "do not share",
            "don.?t share", "do not forward");

    // ---------- Falsche Polizei ----------

    /** Wertsachen, die falsche Polizisten "in Sicherheit bringen" wollen. */
    private static final Pattern WERTSACHEN = muster(
            "bargeld", "schmuck", "wertsachen", "wertgegenstaend", "goldmuenzen", "\\bgold\\b", "erspartes",
            "ersparnisse", "sparbuch", "schliessfach", "tresor", "geld (zu hause|im haus|in der wohnung|daheim)",
            "\\bcash\\b", "jewell?ery", "valuables", "savings");

    private static final Pattern UEBERGABE = muster(
            "abhol", "uebergeb", "uebergabe", "aushaendig", "sicherstell", "in sicherheit", "verwahr", "bereitleg",
            "bereit leg", "vor die (haus)?tuer", "deponier", "mitgeben", "kollege kommt", "beamter kommt",
            "kommt (gleich |nachher |heute )?(vorbei|zu ihnen)", "hand over", "collect", "pick up", "keep it safe");

    private static final Pattern EINBRUCH = muster(
            "einbrecher", "einbruch", "einbrueche", "diebesbande", "bande festgenommen", "raeuber", "ihre adresse "
                    + "(stand|steht) auf", "liste gefunden", "falschgeld", "bankmitarbeiter .{0,30}(betrueg|kriminell)",
            "burglar", "break-?in", "counterfeit");

    private static final Pattern BOTE = muster(
            "beamte", "polizist", "kommissar", "kollege", "kollegin", "kurier", "\\bbote\\b", "mitarbeiter",
            "officer", "courier", "colleague");

    // ---------- Englische Warnwörter ----------
    // Viele Betrugsnachrichten kommen auf Englisch. Die Muster entsprechen den deutschen weiter oben.

    private static final Pattern EN_ZEITDRUCK = muster(
            "\\burgent(ly)?\\b", "immediately", "right away", "as soon as possible", "\\basap\\b",
            "within (\\d+|one|two|24|48) ?(hours?|days?|hrs?)", "final (notice|warning|reminder)", "last (chance|warning)",
            "act now", "expires? (today|soon|in)", "will expire", "\\btoday only\\b", "\\bdeadline\\b");

    private static final Pattern EN_DROHUNG = muster(
            "suspended", "suspension", "\\blocked\\b", "\\bblocked\\b", "restricted", "limited", "deactivated",
            "disabled", "terminated", "will be (closed|deleted|cancelled|canceled)",
            "(unusual|suspicious|unauthorized|unauthorised) (activity|sign-?in|login|access|transaction|payment)",
            "security (alert|issue|problem|reasons)", "(could not|couldn.?t|unable to) (be )?deliver",
            "failed delivery", "delivery (attempt|failed)", "on hold", "returned to sender", "overdue",
            "legal action", "arrest warrant", "\\bfine\\b", "has expired");

    private static final Pattern EN_DATENABFRAGE = muster(
            "(verify|confirm|update|validate|review) your (account|identity|details|information|info|payment|billing|"
                    + "card|address|delivery address|bank|email|phone|profile)",
            "(account|identity|details|information|payment|billing|address) (verification|confirmation|update)",
            "\\blog ?in\\b", "\\bsign ?in\\b", "\\bclick (here|the link|below)\\b", "re-?activate", "\\bunlock\\b",
            "credentials");

    private static final Pattern EN_GEHEIMNIS = muster(
            "password", "passcode", "security code", "verification code", "one-?time (code|password)", "\\botp\\b",
            "card number", "\\bcvv\\b", "\\bcvc\\b", "bank details", "copy of your (id|passport)");

    private static final Pattern EN_GEHEIMNIS_VERLANGT = muster(
            "\\benter\\b", "\\bprovide\\b", "\\bsend\\b", "\\bshare\\b", "\\btell\\b", "reply with", "\\bconfirm\\b",
            "\\bgive\\b", "\\btype\\b", "\\bsubmit\\b");

    private static final Pattern EN_GELDFORDERUNG = muster(
            "\\bpay\\b", "\\bpayment\\b", "\\bfee\\b", "\\bfees\\b", "\\btransfer\\b", "wire", "outstanding (balance|amount)",
            "\\binvoice\\b", "send (me )?(some )?money", "lend me", "customs (charge|duty)", "shipping (fee|cost)",
            "\\bdeposit\\b");

    private static final Pattern EN_BETRAG = Pattern.compile("(?:[€$£] ?\\d+|\\d+(?:[.,]\\d+)? ?(?:usd|gbp|dollars?|pounds?|euros?)\\b)");

    private static final Pattern EN_ZAHLART = muster("gift ?cards?", "voucher codes?", "crypto", "prepaid cards?");

    private static final Pattern EN_ZAHLART_VERLANGT = muster(
            "\\bbuy\\b", "\\bpurchase\\b", "\\bcodes?\\b", "scratch", "\\bpay\\b", "\\bsend\\b", "\\btransfer\\b");

    private static final Pattern EN_FAMILIEN_ANREDE = muster(
            "\\bmum\\b", "\\bmom\\b", "\\bmummy\\b", "\\bdad\\b", "\\bdaddy\\b", "\\bgrandma\\b", "\\bgrandpa\\b",
            "\\bgran\\b", "\\bnan\\b");

    private static final Pattern EN_NUMMERNWECHSEL = muster(
            "new (phone |mobile )?number", "changed my number", "this is my (new )?number", "save this number",
            "delete (my|the) old (one|number)", "temporary number", "(lost|broke|dropped|smashed) my phone",
            "phone (is )?(broken|lost|stolen)", "(my )?phone (fell|went) (in|down)");

    private static final Pattern EN_GEWINN = muster(
            "\\bwon\\b", "\\bwinner\\b", "\\bprize\\b", "\\blottery\\b", "inheritance", "have been selected",
            "you.?ve been chosen", "\\breward\\b", "\\brefund\\b", "tax rebate", "free (iphone|gift|phone)",
            "claim (your|now)", "congratulations");

    private static final Pattern EN_FERNZUGRIFF = muster(
            "remote access", "remote (support|control)", "(microsoft|windows|apple) (support|technician|security)",
            "tech(nical)? support", "\\bvirus\\b", "\\binfected\\b", "\\bmalware\\b", "\\bhacked\\b");

    private static final Pattern EN_INSTALLIEREN = muster("\\binstall\\b", "\\bdownload\\b");

    private static final Pattern EN_SCHNELLES_GELD = muster(
            "work from home", "part-?time job", "job offer", "(earn|make) .{0,20}(per|a) (day|week|hour)",
            "\\binvest", "guaranteed (profit|return)", "passive income", "easy money", "financial freedom",
            "\\btrading\\b", "\\brecruiter\\b", "\\bhr department\\b");

    private static final Pattern EN_ERPRESSUNG = muster(
            "\\bwebcam\\b", "recorded you", "recording of you", "video of you", "your password is", "adult (site|website)",
            "i have access to your (device|computer|phone)", "\\bpublish\\b", "send (it|the video) to (all )?your contacts");

    private static final Pattern EN_ANRUF_VERLANGT = muster(
            "\\bcall (us|me|this number|now|back|immediately)\\b", "\\bphone us\\b", "contact us", "helpline", "hotline");

    private static final Pattern EN_BANK = muster(
            "\\bbank\\b", "bank account", "\\bbanking\\b", "credit card", "debit card", "\\bcard\\b", "paypal",
            "\\biban\\b", "\\bvisa\\b", "mastercard");

    private static final Pattern EN_PAKET = muster(
            "\\bparcel\\b", "\\bpackage\\b", "\\bdelivery\\b", "\\bshipment\\b", "\\bcourier\\b", "\\bcustoms\\b",
            "tracking", "\\bdeliver");

    private static final Pattern EN_BEHOERDE = muster(
            "\\btax\\b", "tax office", "\\bhmrc\\b", "\\birs\\b", "\\bcourt\\b", "the police", "police (officer|department)",
            "prosecutor", "\\bgovernment\\b", "\\bwarrant\\b");

    private static final Pattern EN_KUNDENKONTO = muster(
            "your account", "subscription", "membership", "apple id", "\\bprofile\\b", "storage (is )?(full|almost)",
            "mailbox");

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
        boolean zeitdruck = merke(merkmale, Merkmal.ZEITDRUCK, findet(ZEITDRUCK, t) || findet(EN_ZEITDRUCK, t));
        boolean drohung = merke(merkmale, Merkmal.DROHUNG, findet(DROHUNG, t) || findet(EN_DROHUNG, t));
        boolean datenabfrage = merke(merkmale, Merkmal.DATENABFRAGE, findet(DATENABFRAGE, t) || findet(EN_DATENABFRAGE, t));
        boolean geheimnis = merke(merkmale, Merkmal.GEHEIME_DATEN,
                !findet(GEHEIMNIS_WARNUNG, t) && !findet(CODE_WARNUNG, t)
                        && ((findet(GEHEIMNIS, t) && findet(GEHEIMNIS_VERLANGT, t))
                        || (findet(EN_GEHEIMNIS, t) && findet(EN_GEHEIMNIS_VERLANGT, t))
                        || findet(CODE_WEITERGABE, t)));
        boolean geld = merke(merkmale, Merkmal.GELDFORDERUNG, findet(GELDFORDERUNG, t) || findet(EN_GELDFORDERUNG, t));
        boolean betrag = BETRAG.matcher(t).find() || EN_BETRAG.matcher(t).find();
        boolean zahlart = merke(merkmale, Merkmal.UNGEWOEHNLICHE_ZAHLART,
                (findet(ZAHLART, t) || findet(EN_ZAHLART, t))
                        && (findet(ZAHLART_VERLANGT, t) || findet(EN_ZAHLART_VERLANGT, t)));
        boolean familie = findet(FAMILIEN_ANREDE, t) || findet(EN_FAMILIEN_ANREDE, t);
        boolean handyKaputt = findet(HANDY_KAPUTT, t);
        boolean neueNummer = merke(merkmale, Merkmal.NEUE_NUMMER,
                familie && !findet(NUMMER_BLEIBT, t)
                        && (findet(NUMMERNWECHSEL, t) || findet(EN_NUMMERNWECHSEL, t)
                        || (handyKaputt && t.contains("nummer"))));
        boolean gewinn = merke(merkmale, Merkmal.GEWINN, findet(GEWINN, t) || findet(EN_GEWINN, t));
        boolean rueckruf = merke(merkmale, Merkmal.RUECKRUF,
                TELEFONNUMMER.matcher(t).find()
                        && (findet(ANRUF_VERLANGT, t) || findet(EN_ANRUF_VERLANGT, t)));
        boolean infektion = findet(INFEKTION, t) || findet(EN_FERNZUGRIFF, t);
        boolean fernzugriff = merke(merkmale, Merkmal.FERNZUGRIFF, findet(FERNZUGRIFF_PROGRAMM, t)
                || (infektion && (rueckruf || fremderLink || findet(INSTALLIEREN, t) || findet(EN_INSTALLIEREN, t))));
        boolean schnellesGeld = merke(merkmale, Merkmal.SCHNELLES_GELD, findet(SCHNELLES_GELD, t) || findet(EN_SCHNELLES_GELD, t));
        boolean schock = merke(merkmale, Merkmal.SCHOCK, findet(SCHOCK, t) && (geld || betrag));
        boolean erpressung = merke(merkmale, Merkmal.ERPRESSUNG,
                (findet(ERPRESSUNG, t) || findet(EN_ERPRESSUNG, t))
                        && (geld || betrag || findet(ZAHLART, t) || findet(EN_ZAHLART, t)));
        // Falsche Polizei: Wertsachen zusammen mit Polizei, Einbruch-Geschichte oder einem Abholer
        boolean falschePolizei = merke(merkmale, Merkmal.FALSCHE_POLIZEI, findet(WERTSACHEN, t)
                && (findet(POLIZEI, t) || findet(EN_BEHOERDE, t) || findet(EINBRUCH, t)
                        || (findet(UEBERGABE, t) && findet(BOTE, t))));

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
                || falschePolizei
                // Geld auf ein "sicheres Konto" oder Geld an die "Polizei": immer Betrug
                || findet(SICHERES_KONTO, t)
                || (findet(POLIZEI, t) && geld)
                || (schnellesGeld && (fremderLink || findet(KONTAKT_MESSENGER, t) || rueckruf
                        || t.contains("contact us") || t.contains("message us") || t.contains("reply to")))
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
        long bank = zaehle(BANK, t) + zaehle(EN_BANK, t);
        long paket = zaehle(PAKET, t) + zaehle(EN_PAKET, t);
        long behoerde = zaehle(BEHOERDE, t) + zaehle(EN_BEHOERDE, t);
        long kundenkonto = zaehle(KUNDENKONTO, t) + zaehle(EN_KUNDENKONTO, t);
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
