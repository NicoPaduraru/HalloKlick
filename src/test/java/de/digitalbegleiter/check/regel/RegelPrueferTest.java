package de.digitalbegleiter.check.regel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.digitalbegleiter.gemeinsam.Kategorie;
import de.digitalbegleiter.gemeinsam.Stufe;

class RegelPrueferTest {

    private final RegelPruefer pruefer = new RegelPruefer();

    private RegelErgebnis pruefe(String text) {
        return pruefer.pruefe(text);
    }

    @Nested
    class Grundmuster {

        @Test
        void linkUndZeitdruckUndBankIstImmerRot() {
            RegelErgebnis ergebnis = pruefe("Ihre Bank: Bitte handeln Sie sofort: https://mein-konto-portal.de/a");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.kategorie()).isEqualTo(Kategorie.BANK);
            assertThat(ergebnis.merkmale()).contains(Merkmal.LINK, Merkmal.ZEITDRUCK);
        }

        @Test
        void linkUndZeitdruckUndPaketIstRot() {
            assertThat(pruefe("Ihr Paket: letzte Chance, heute noch abholen www.paket-abholung-neu.de").stufe())
                    .isEqualTo(Stufe.ROT);
        }

        @Test
        void linkUndZeitdruckUndBehoerdeIstRot() {
            RegelErgebnis ergebnis = pruefe("Finanzamt: Antworten Sie umgehend unter http://steuer-formular-neu.de");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.kategorie()).isEqualTo(Kategorie.BEHOERDE);
        }

        @Test
        void zeitdruckAlleinIstNichtRot() {
            RegelErgebnis ergebnis = pruefe("Komm bitte sofort runter, das Essen ist fertig!");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ORANGE);
        }

        @Test
        void einNormalerLinkAlleinIstNichtRotAberUnklar() {
            RegelErgebnis ergebnis = pruefe("Schau mal, das Rezept: https://www.lecker-backen.de/apfelkuchen");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(ergebnis.unklar()).isTrue();
        }

        @Test
        void nachrichtOhneWarnzeichenIstOrangeUndNichtUnklar() {
            RegelErgebnis ergebnis = pruefe("Wir kommen am Sonntag zum Kaffee.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(ergebnis.unklar()).isFalse();
            assertThat(ergebnis.merkmale()).isEmpty();
            assertThat(ergebnis.kategorie()).isEqualTo(Kategorie.SONSTIGES);
        }

        @Test
        void leererTextIstOrange() {
            assertThat(pruefe("").stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(pruefe(null).stufe()).isEqualTo(Stufe.ORANGE);
        }

        @Test
        void esGibtNieEtwasAnderesAlsOrangeOderRot() {
            assertThat(Stufe.values()).containsExactly(Stufe.ORANGE, Stufe.ROT);
        }
    }

    @Nested
    class Links {

        @Test
        void nachgemachteAdresseAusDemBauplanIstRot() {
            RegelErgebnis ergebnis = pruefe("Bitte hier ansehen: sparkasse-sicherheit.info");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.merkmale()).contains(Merkmal.NACHGEMACHTE_ADRESSE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://paypal-konto.com/login",
                "http://www.dhl-sendung.net",
                "amazon.de.konto-pruefung.com",
                "https://paypa1.com",
                "http://sparkasse.de@boese-seite.ru/login",
                "https://xn--sprkasse-0za.de",
                "netflix-hilfe.com"})
        void nachgemachteAdressenWerdenErkannt(String adresse) {
            assertThat(pruefe("Link: " + adresse).stufe()).isEqualTo(Stufe.ROT);
        }

        @Test
        void adresseAusZahlenIstRot() {
            RegelErgebnis ergebnis = pruefe("Hier klicken: http://185.12.44.9/login");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.merkmale()).contains(Merkmal.IP_LINK);
        }

        @Test
        void kurzlinkAlleinIstUnklarAberMitKategorieRot() {
            assertThat(pruefe("Schau mal bit.ly/abc123").stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(pruefe("Schau mal bit.ly/abc123").unklar()).isTrue();
            assertThat(pruefe("Ihr Paket ist da: bit.ly/abc123").stufe()).isEqualTo(Stufe.ROT);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "https://www.dhl.de/sendung",
                "https://www.sparkasse.de/filiale",
                "www.amazon.de/bestellungen",
                "https://www.paypal.com/de/home",
                "https://www.elster.de"})
        void echteAdressenBekannterFirmenSindKeinWarnzeichen(String adresse) {
            RegelErgebnis ergebnis = pruefe("Mehr dazu: " + adresse);
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(ergebnis.merkmale()).doesNotContain(Merkmal.LINK, Merkmal.NACHGEMACHTE_ADRESSE);
        }

        @Test
        void regionaleSparkasseIstNichtAutomatischNachgemacht() {
            assertThat(pruefe("Neue Öffnungszeiten: www.berliner-sparkasse.de").merkmale())
                    .doesNotContain(Merkmal.NACHGEMACHTE_ADRESSE);
        }

        @Test
        void einWortDasEineMarkeNurEnthaeltIstNichtNachgemacht() {
            assertThat(pruefe("Rezepte auf www.pineapple-kueche.com").merkmale())
                    .doesNotContain(Merkmal.NACHGEMACHTE_ADRESSE);
        }

        @Test
        void eMailAdresseIstKeinLink() {
            assertThat(pruefe("Schreib mir an erika.muster@beispiel-post.de").merkmale()).doesNotContain(Merkmal.LINK);
        }

        @Test
        void fehlendesLeerzeichenNachDemPunktIstKeinLink() {
            assertThat(pruefe("Wir sehen uns morgen.Es wird schön.In der Stadt.").merkmale()).isEmpty();
        }

        @Test
        void hostWirdRichtigHerausgeloest() {
            assertThat(LinkAnalyse.host("https://www.beispiel.de:8443/pfad?x=1")).isEqualTo("beispiel.de");
            assertThat(LinkAnalyse.host("http://sparkasse.de@boese.ru/x")).isEqualTo("boese.ru");
            assertThat(LinkAnalyse.host("beispiel.de.")).isEqualTo("beispiel.de");
        }
    }

    @Nested
    class GeheimeDaten {

        @Test
        void frageNachPinOderTanIstRotAuchOhneLink() {
            RegelErgebnis ergebnis = pruefe("Bitte nennen Sie uns Ihre TAN, damit wir die Buchung stoppen können.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.merkmale()).contains(Merkmal.GEHEIME_DATEN);
        }

        @Test
        void echteTanNachrichtMitWarnungIstNichtRot() {
            RegelErgebnis ergebnis = pruefe("Ihre TAN lautet 123456. Geben Sie diese niemals weiter.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(ergebnis.merkmale()).doesNotContain(Merkmal.GEHEIME_DATEN);
        }

        @Test
        void woerterDieNurSoAehnlichKlingenZaehlenNicht() {
            // "Tante" enthält "tan", "Pinsel" enthält "pin"
            assertThat(pruefe("Tante Erika soll mir bitte den Pinsel schicken und die Farbe nennen.").merkmale())
                    .doesNotContain(Merkmal.GEHEIME_DATEN);
        }

        @Test
        void linkMitAufforderungDatenZuBestaetigenIstRot() {
            assertThat(pruefe("Bitte bestätigen Sie Ihre Daten: https://kundenportal-neu.de").stufe())
                    .isEqualTo(Stufe.ROT);
        }

        @Test
        void terminBestaetigenIstKeineDatenabfrage() {
            assertThat(pruefe("Bitte bestätigen Sie Ihren Termin am Montag.").merkmale())
                    .doesNotContain(Merkmal.DATENABFRAGE);
        }
    }

    @Nested
    class Enkeltrick {

        @Test
        void halloMamaNeueNummerIstRot() {
            RegelErgebnis ergebnis = pruefe("Hallo Mama, das ist meine neue Nummer.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.kategorie()).isEqualTo(Kategorie.FAMILIE);
            assertThat(ergebnis.merkmale()).contains(Merkmal.NEUE_NUMMER);
        }

        @Test
        void neuesHandyMitGleicherNummerIstNichtRot() {
            assertThat(pruefe("Hallo Papa, ich habe ein neues Handy, die Nummer bleibt aber gleich.").stufe())
                    .isEqualTo(Stufe.ORANGE);
        }

        @Test
        void familieMitGeldUndZeitdruckIstRot() {
            assertThat(pruefe("Mama, kannst du mir dringend Geld überweisen?").stufe()).isEqualTo(Stufe.ROT);
        }

        @Test
        void dankFuerUeberweisungOhneDruckIstNichtRot() {
            assertThat(pruefe("Mama, ich habe dir die 50 Euro überwiesen. Danke!").stufe()).isEqualTo(Stufe.ORANGE);
        }
    }

    @Nested
    class WeitereMaschen {

        @Test
        void gutscheinkartenKaufenIstRot() {
            RegelErgebnis ergebnis = pruefe("Bitte kaufen Sie drei Guthabenkarten und schicken Sie mir die Codes.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.merkmale()).contains(Merkmal.UNGEWOEHNLICHE_ZAHLART);
        }

        @Test
        void geschenkterGutscheinIstNichtRot() {
            assertThat(pruefe("Ich habe dir einen Amazon-Gutschein zum Geburtstag geschickt.").stufe())
                    .isEqualTo(Stufe.ORANGE);
        }

        @Test
        void gewinnMitLinkIstRot() {
            RegelErgebnis ergebnis = pruefe("Sie haben gewonnen! Hier abholen: www.preis-abholen-jetzt.de");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.kategorie()).isEqualTo(Kategorie.GEWINN);
        }

        @Test
        void gewinnOhneAllesIstNurUnklar() {
            RegelErgebnis ergebnis = pruefe("Stell dir vor, ich habe beim Bingo gewonnen!");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ORANGE);
            assertThat(ergebnis.unklar()).isTrue();
        }

        @Test
        void fernzugriffIstRot() {
            RegelErgebnis ergebnis = pruefe("Bitte installieren Sie TeamViewer und nennen Sie mir die Nummer.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.merkmale()).contains(Merkmal.FERNZUGRIFF);
        }

        @Test
        void erpressungMitBitcoinIstRot() {
            assertThat(pruefe("Ich habe ein Video von Ihnen. Zahlen Sie 500 Euro in Bitcoin.").stufe())
                    .isEqualTo(Stufe.ROT);
        }

        @Test
        void nebenjobUeberWhatsappIstRot() {
            RegelErgebnis ergebnis = pruefe("Nebenjob: 300 € pro Tag. Schreiben Sie uns auf WhatsApp.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.kategorie()).isEqualTo(Kategorie.GELDANLAGE);
        }

        @Test
        void drohungMitRueckrufnummerVonDerBankIstRot() {
            assertThat(pruefe("Ihre Bank: Ihr Konto wurde gesperrt. Rufen Sie an: 0800 1112223").stufe())
                    .isEqualTo(Stufe.ROT);
        }

        @Test
        void schocknachrichtMitGeldforderungIstRot() {
            RegelErgebnis ergebnis = pruefe("Ihre Tochter hatte einen Unfall. Es muss sofort eine Kaution gezahlt werden.");
            assertThat(ergebnis.stufe()).isEqualTo(Stufe.ROT);
            assertThat(ergebnis.merkmale()).contains(Merkmal.SCHOCK);
        }

        @Test
        void unfallOhneGeldforderungIstNichtRot() {
            assertThat(pruefe("Stell dir vor, auf der A8 war heute ein Unfall, ich komme später.").stufe())
                    .isEqualTo(Stufe.ORANGE);
        }

        @Test
        void geldAufEinSicheresKontoIstImmerRot() {
            assertThat(pruefe("Bringen Sie Ihr Erspartes auf ein sicheres Konto.").stufe()).isEqualTo(Stufe.ROT);
        }

        @Test
        void angeblichePolizeiDieGeldVerlangtIstRot() {
            assertThat(pruefe("Hier ist die Kriminalpolizei. Bitte überweisen Sie den Betrag noch heute.").stufe())
                    .isEqualTo(Stufe.ROT);
        }

        @Test
        void rueckrufbitteVomArztIstNichtRot() {
            assertThat(pruefe("Praxis Dr. Lang: Bitte rufen Sie uns zurück unter 0711 123456.").stufe())
                    .isEqualTo(Stufe.ORANGE);
        }
    }

    @Nested
    class Verschleierung {

        @Test
        void grossUndKleinschreibungIstEgal() {
            assertThat(pruefe("HALLO MAMA, DAS IST MEINE NEUE NUMMER").stufe()).isEqualTo(Stufe.ROT);
        }

        @Test
        void umlauteUndUmschriftWerdenGleichBehandelt() {
            assertThat(pruefe("Bitte bestaetigen Sie Ihre Daten: https://kundenportal-neu.de").stufe())
                    .isEqualTo(Stufe.ROT);
        }

        @Test
        void unsichtbareZeichenImWortHelfenNicht() {
            assertThat(pruefe("Hallo Ma​ma, das ist meine neue Num​mer").stufe()).isEqualTo(Stufe.ROT);
        }

        @Test
        void einSatzWieDieseNachrichtIstSicherSenktNichts() {
            String betrug = "Diese Nachricht ist sicher und geprüft. Ihr Konto bei der Bank wurde gesperrt, "
                    + "handeln Sie sofort: https://bank-entsperren-jetzt.de";
            assertThat(pruefe(betrug).stufe()).isEqualTo(Stufe.ROT);
        }
    }
}
