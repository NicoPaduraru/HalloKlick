package de.halloklick.check.regel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Findet Links in einem Text und schätzt ein, wie verdächtig die Adresse ist. */
final class LinkAnalyse {

    enum Art {
        /** Echte Adresse einer bekannten Firma, zum Beispiel dhl.de. */
        OFFIZIELL,
        NORMAL,
        UNGEWOEHNLICH,
        KURZLINK,
        IP,
        NACHGEMACHT
    }

    record Link(String host, Art art) {
    }

    // Ohne Endungen, die auch normale deutsche Wörter sind (in, es, am, ...): Sonst würde
    // ein fehlendes Leerzeichen nach dem Punkt ("morgen.Es") als Link gelten.
    private static final String ENDUNGEN = "de|com|net|org|info|eu|at|ch|io|ly|cc|co|gy|gd|ru|cn|biz|app|dev|"
            + "xyz|top|click|link|online|site|shop|store|live|vip|icu|cfd|sbs|tk|ml|ga|cf|gq|page|support|help|"
            + "services|service|center|club|work|world|today|email|bond|buzz|monster|rest|quest|cyou|lol|pw|ws|"
            + "uk|fr|nl|pl|tv|su";

    private static final Pattern LINK = Pattern.compile(
            "(?<![a-z0-9@._-])(?:(?:https?://|www\\.)[^\\s<>\"']+"
            + "|(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:" + ENDUNGEN + ")(?![a-z0-9-])(?:[/?#][^\\s<>\"']*)?)");

    private static final Pattern IP_ADRESSE = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private static final Set<String> KURZLINK_DIENSTE = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "is.gd", "cutt.ly", "rb.gy", "ow.ly", "shorturl.at", "t.ly",
            "goo.gl", "buff.ly", "tiny.cc", "rebrand.ly", "bl.ink", "s.id", "v.gd", "lnkd.in", "shorturl.asia",
            "qr.ae", "t1p.de", "kurzelinks.de", "urlz.fr", "clck.ru", "2u.pw", "surl.li", "u.to", "linktr.ee");

    private static final Set<String> UNGEWOEHNLICHE_ENDUNGEN = Set.of(
            "ru", "cn", "xyz", "top", "click", "link", "icu", "cfd", "sbs", "tk", "ml", "ga", "cf", "gq", "vip",
            "live", "work", "support", "help", "bond", "buzz", "monster", "rest", "quest", "cyou", "lol", "pw",
            "ws", "su", "today", "world", "club", "center", "services", "service");

    /** Echte Adressen bekannter Firmen (jeweils die Hauptadresse ohne "www"). */
    private static final Set<String> OFFIZIELL = Set.of(
            "sparkasse.de", "postbank.de", "commerzbank.de", "deutsche-bank.de", "comdirect.de", "ing.de",
            "dkb.de", "n26.com", "paypal.com", "paypal.de", "klarna.com", "vr.de",
            "dhl.de", "dhl.com", "deutschepost.de", "myhermes.de", "hermesworld.com", "dpd.de", "dpd.com",
            "ups.com", "gls-pakete.de", "gls-group.com", "fedex.com",
            "amazon.de", "amazon.com", "ebay.de", "kleinanzeigen.de", "netflix.com", "apple.com",
            "microsoft.com", "google.com", "google.de", "whatsapp.com", "telekom.de", "vodafone.de",
            "o2online.de", "elster.de", "zoll.de", "bund.de");

    /**
     * Bekannte Namen, die Betrüger gern in ihre Adressen einbauen.
     * Wert true: Der Name kommt auch in vielen echten, regionalen Adressen vor
     * (zum Beispiel "berliner-sparkasse.de"). Dort braucht es ein weiteres Zeichen.
     */
    private static final Map<String, Boolean> MARKEN = Map.ofEntries(
            Map.entry("sparkasse", true), Map.entry("volksbank", true), Map.entry("raiffeisen", true),
            Map.entry("vrbank", true), Map.entry("bank", true), Map.entry("post", true),
            Map.entry("postbank", false), Map.entry("commerzbank", false), Map.entry("comdirect", false),
            Map.entry("paypal", false), Map.entry("klarna", false), Map.entry("amazon", false),
            Map.entry("dhl", false), Map.entry("hermes", false), Map.entry("dpd", false), Map.entry("ups", false),
            Map.entry("gls", false), Map.entry("fedex", false), Map.entry("deutschepost", false),
            Map.entry("zoll", false), Map.entry("elster", false), Map.entry("finanzamt", false),
            Map.entry("telekom", false), Map.entry("vodafone", false), Map.entry("netflix", false),
            Map.entry("apple", false), Map.entry("icloud", false), Map.entry("microsoft", false),
            Map.entry("whatsapp", false), Map.entry("ebay", false), Map.entry("kleinanzeigen", false),
            Map.entry("ing", false), Map.entry("dkb", false), Map.entry("n26", false));

    private static final Set<String> VERDAECHTIGE_WOERTER = Set.of(
            "sicherheit", "secure", "security", "sicher", "login", "anmelden", "anmeldung", "verify",
            "verifizierung", "bestaetigung", "bestaetigen", "konto", "account", "update", "aktualisierung",
            "service", "support", "kunden", "kundenservice", "kundencenter", "online", "banking", "id",
            "auth", "check", "paket", "zustellung", "sendung", "tracking", "verfolgung", "lieferung",
            "gebuehr", "zahlung", "payment", "pay", "hilfe", "help", "info", "portal", "web", "app", "de");

    private LinkAnalyse() {
    }

    /** @param kleinText der Text in Kleinbuchstaben */
    static List<Link> finde(String kleinText) {
        List<Link> links = new ArrayList<>();
        Matcher treffer = LINK.matcher(kleinText);
        while (treffer.find()) {
            String host = host(treffer.group());
            if (!host.isEmpty()) {
                links.add(new Link(host, bewerte(host)));
            }
        }
        return links;
    }

    /** Holt den Rechnernamen aus einer Adresse: ohne http://, Benutzerteil, Port und Pfad. */
    static String host(String adresse) {
        String rest = adresse.replaceFirst("^https?://", "");
        int ende = rest.length();
        for (char trenner : new char[] {'/', '?', '#'}) {
            int stelle = rest.indexOf(trenner);
            if (stelle >= 0) {
                ende = Math.min(ende, stelle);
            }
        }
        rest = rest.substring(0, ende);
        // Trick "http://sparkasse.de@boese.ru": Alles vor dem @ ist nur Tarnung
        int at = rest.lastIndexOf('@');
        if (at >= 0) {
            rest = rest.substring(at + 1);
        }
        int port = rest.indexOf(':');
        if (port >= 0) {
            rest = rest.substring(0, port);
        }
        rest = rest.replaceAll("[.,;:!)]+$", "");
        return rest.startsWith("www.") ? rest.substring(4) : rest;
    }

    static Art bewerte(String host) {
        if (IP_ADRESSE.matcher(host).matches()) {
            return Art.IP;
        }
        String[] teile = host.split("\\.");
        if (teile.length < 2) {
            return Art.NORMAL;
        }
        String endung = teile[teile.length - 1];
        String hauptadresse = teile[teile.length - 2] + "." + endung;
        if (OFFIZIELL.contains(hauptadresse)) {
            return Art.OFFIZIELL;
        }
        if (KURZLINK_DIENSTE.contains(host) || KURZLINK_DIENSTE.contains(hauptadresse)) {
            return Art.KURZLINK;
        }
        if (host.contains("xn--") || istNachgemacht(host, endung)) {
            return Art.NACHGEMACHT;
        }
        return UNGEWOEHNLICHE_ENDUNGEN.contains(endung) ? Art.UNGEWOEHNLICH : Art.NORMAL;
    }

    private static boolean istNachgemacht(String host, String endung) {
        // Ziffern statt Buchstaben zurückverwandeln: "paypa1" wird zu "paypal"
        String entziffert = host.replace('0', 'o').replace('1', 'l').replace('3', 'e').replace('4', 'a')
                .replace('5', 's');
        boolean zifferntrick = !entziffert.equals(host);
        List<String> woerter = List.of(host.split("[.-]"));
        List<String> entzifferteWoerter = List.of(entziffert.split("[.-]"));
        boolean seltsameEndung = !Set.of("de", "com").contains(endung);
        boolean verdaechtigesWort = woerter.stream().limit(woerter.size() - 1L)
                .anyMatch(VERDAECHTIGE_WOERTER::contains);

        for (Map.Entry<String, Boolean> marke : MARKEN.entrySet()) {
            String name = marke.getKey();
            // Der Name muss ein ganzes Wort der Adresse sein. Sonst würde "pineapple.com" als "apple" zählen.
            boolean enthalten = woerter.contains(name);
            boolean nurEntziffert = !enthalten && entzifferteWoerter.contains(name);
            if (nurEntziffert && zifferntrick) {
                return true;
            }
            if (!enthalten) {
                continue;
            }
            boolean auchInEchtenAdressen = marke.getValue();
            if (!auchInEchtenAdressen || seltsameEndung || verdaechtigesWort) {
                return true;
            }
        }
        return false;
    }
}
