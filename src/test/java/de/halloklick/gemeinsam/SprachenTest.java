package de.halloklick.gemeinsam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import de.halloklick.IntegrationsTest;
import de.halloklick.check.regel.Merkmal;
import de.halloklick.familie.HilfeThema;
import de.halloklick.ki.PruefEingabe;

/** Mehrere Sprachen: Deutsch, Englisch, Italienisch. */
class SprachenTest extends IntegrationsTest {

    private static final String BETRUG = "Sparkasse: Ihr Konto wurde gesperrt. Bitte bestätigen Sie Ihre Daten "
            + "sofort unter https://sparkasse-sicherheit.info/login";

    @Autowired
    private Texte texte;

    // ---------- Vollständigkeit der Übersetzungen ----------

    @ParameterizedTest
    @EnumSource(value = Sprache.class, names = "DE", mode = EnumSource.Mode.EXCLUDE)
    void jedesWarnzeichenUndJedesThemaIstUebersetzt(Sprache sprache) {
        List<String> deutsch = new ArrayList<>();
        for (Merkmal merkmal : Merkmal.values()) {
            deutsch.add(merkmal.erklaerung());
        }
        for (HilfeThema thema : HilfeThema.values()) {
            deutsch.add(thema.text());
        }
        assertThat(deutsch).allSatisfy(satz ->
                assertThat(texte.in(sprache, satz)).as("Übersetzung fehlt: " + satz).isNotEqualTo(satz));
    }

    @Test
    void alleSprachenUebersetzenDieselbenSaetze() {
        Set<String> englisch = texte.alle(Sprache.EN).keySet();
        assertThat(englisch).isNotEmpty();
        for (Sprache sprache : Sprache.values()) {
            if (sprache != Sprache.DE) {
                assertThat(texte.alle(sprache).keySet()).as(sprache.name()).isEqualTo(englisch);
            }
        }
    }

    /**
     * Jeder deutsche Satz, den der Server als Fehlermeldung verschickt, braucht eine Übersetzung.
     * Der Test sucht diese Sätze direkt im Quellcode.
     */
    @Test
    void alleFehlermeldungenImQuellcodeSindUebersetzt() throws IOException {
        Pattern satz = Pattern.compile(
                "(?:message = |ApiFehler\\.\\w+\\(|new ApiFehler\\([\\w.]+,\\s*)\"((?:[^\"\\\\]|\\\\.)*)\"\\s*[,)]");
        Set<String> fehlend = new TreeSet<>();
        Set<String> englisch = texte.alle(Sprache.EN).keySet();
        try (var dateien = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/java"))) {
            for (var datei : dateien.filter(p -> p.toString().endsWith(".java")).toList()) {
                // Zusammengesetzte Sätze ("..." + "...") erst zu einem Stück machen
                String quelle = java.nio.file.Files.readString(datei).replaceAll("\"\\s*\\n\\s*\\+ \"", "");
                Matcher treffer = satz.matcher(quelle);
                while (treffer.find()) {
                    if (!englisch.contains(treffer.group(1))) {
                        fehlend.add(treffer.group(1));
                    }
                }
            }
        }
        assertThat(fehlend).as("Sätze ohne Übersetzung in sprachen/en.json").isEmpty();
    }

    @Test
    void dieTextdateienDerWebAppHabenInJederSpracheDieselbenSchluessel() throws IOException {
        Set<String> deutsch = schluesselAus("de");
        assertThat(deutsch).hasSizeGreaterThan(100);
        for (Sprache sprache : Sprache.values()) {
            assertThat(schluesselAus(sprache.kuerzel())).as(sprache.name()).isEqualTo(deutsch);
        }
    }

    private static Set<String> schluesselAus(String kuerzel) throws IOException {
        try (InputStream datei = SprachenTest.class.getResourceAsStream("/static/js/sprachen/" + kuerzel + ".js")) {
            assertThat(datei).as("Textdatei für " + kuerzel).isNotNull();
            String inhalt = new String(datei.readAllBytes(), StandardCharsets.UTF_8);
            Set<String> schluessel = new TreeSet<>();
            Matcher treffer = Pattern.compile("(?m)^\\s*'([\\w.]+)':").matcher(inhalt);
            while (treffer.find()) {
                schluessel.add(treffer.group(1));
            }
            return schluessel;
        }
    }

    // ---------- Sprache der Anfrage ----------

    @Test
    void ohneAngabeUndBeiUnbekannterSpracheAntwortetDerServerAufDeutsch() throws Exception {
        mvc.perform(get("/api/ich")).andExpect(jsonPath("$.detail").value("Bitte melden Sie sich an."));
        mvc.perform(get("/api/ich").header("Accept-Language", "fi"))
                .andExpect(jsonPath("$.detail").value("Bitte melden Sie sich an."));
        assertThat(Sprache.von(Locale.JAPANESE)).isEqualTo(Sprache.DE);
        assertThat(Sprache.von("it-CH")).isEqualTo(Sprache.IT);
    }

    @Test
    void fehlermeldungenKommenInDerGewaehltenSprache() throws Exception {
        mvc.perform(get("/api/ich").header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Please sign in."));
        mvc.perform(get("/api/ich").header("Accept-Language", "it-IT,it;q=0.9"))
                .andExpect(jsonPath("$.detail").value("Per favore effettui l'accesso."));

        // Auch die Prüfung der Eingaben
        mvc.perform(post("/api/auth/registrieren").with(csrfKopf()).header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kurz@example.org\",\"passwort\":\"kurz\",\"anzeigename\":\"K\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The password must be between 10 and 72 characters long."));
    }

    @Test
    void dasErgebnisDesSicherheitschecksIstUebersetzt() throws Exception {
        Geraet geraet = neuesGeraet();
        mvc.perform(multipart("/api/checks").param("text", BETRUG).with(csrfKopf()).cookie(geraet.cookie())
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.titel").value("Careful! This looks like a scam."))
                .andExpect(jsonPath("$.merkmale", hasItem("The message contains a link.")))
                .andExpect(jsonPath("$.naechsterSchritt").value(
                        "Do not tap on the link. Call your bank. Use the number printed on your bank card."))
                .andExpect(jsonPath("$.hinweis").value(org.hamcrest.Matchers.startsWith("This is an assessment")));

        mvc.perform(multipart("/api/checks").param("text", BETRUG).with(csrfKopf()).cookie(geraet.cookie())
                        .header("Accept-Language", "it"))
                .andExpect(jsonPath("$.titel").value("Attenzione! Sembra una truffa."));
    }

    @Test
    void dieKiErfaehrtInWelcherSpracheSieAntwortenSoll() throws Exception {
        kiAntwortet(Stufe.ORANGE, Kategorie.SONSTIGES, "Nothing unusual.", "");
        mvc.perform(multipart("/api/checks").param("text", "Schau mal: https://www.irgendeine-seite-xy.de/a")
                        .with(csrfKopf()).cookie(neuesGeraet().cookie()).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.erklaerung").value("Nothing unusual."));

        ArgumentCaptor<PruefEingabe> eingabe = ArgumentCaptor.forClass(PruefEingabe.class);
        org.mockito.Mockito.verify(kiPruefer).pruefe(eingabe.capture());
        assertThat(eingabe.getValue().antwortSprache()).isEqualTo("Englisch");
    }

    // ---------- Anleitungen ----------

    @ParameterizedTest
    @EnumSource(Sprache.class)
    void jedeSpracheHatGleichVieleAnleitungenUndJederSchrittEinBild(Sprache sprache) {
        Integer anleitungen = jdbc.queryForObject(
                "SELECT count(*) FROM anleitung WHERE quelle = 'SYSTEM' AND sprache = ?", Integer.class,
                sprache.kuerzel());
        assertThat(anleitungen).isEqualTo(37);

        Integer ohneBild = jdbc.queryForObject("""
                SELECT count(*) FROM anleitung_schritt s JOIN anleitung a ON a.id = s.anleitung_id
                WHERE a.quelle = 'SYSTEM' AND a.sprache = ? AND (s.bild_pfad IS NULL OR s.text = '')
                """, Integer.class, sprache.kuerzel());
        assertThat(ohneBild).isZero();
    }

    @Test
    void zuJedemBildGibtEsEineDatei() {
        List<String> bilder = jdbc.queryForList(
                "SELECT DISTINCT bild_pfad FROM anleitung_schritt WHERE bild_pfad IS NOT NULL", String.class);
        assertThat(bilder).isNotEmpty().allSatisfy(bild -> {
            assertThat(bild).matches("[a-z0-9-]+");
            assertThat(SprachenTest.class.getResource("/static/bilder/" + bild + ".svg"))
                    .as("Bilddatei " + bild + ".svg").isNotNull();
        });
    }

    @Test
    void anleitungenKommenInDerGewaehltenSprache() throws Exception {
        mvc.perform(get("/api/anleitungen").param("suche", "send photo").param("plattform", "ANDROID")
                        .header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titel").value("Send a photo with WhatsApp"))
                .andExpect(jsonPath("$[0].bildPfad").value("chat-app"));

        mvc.perform(get("/api/anleitungen").param("suche", "inviare foto").param("plattform", "IOS")
                        .header("Accept-Language", "it"))
                .andExpect(jsonPath("$[0].titel").value("Inviare una foto con WhatsApp"));

        // Englische Suche findet keine deutschen Anleitungen und umgekehrt
        mvc.perform(get("/api/anleitungen").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.length()", greaterThan(20)))
                .andExpect(jsonPath("$[*].titel", everyItem(matchesPattern("[\\x00-\\x7F’“”]+"))));
        mvc.perform(get("/api/anleitungen").param("suche", "Taschenlampe").header("Accept-Language", "en"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dieSchritteEinerAnleitungHabenBilder() throws Exception {
        MvcResult suche = mvc.perform(get("/api/anleitungen").param("suche", "Notruf")).andReturn();
        String id = json.readTree(suche.getResponse().getContentAsString()).get(0).get("id").asText();

        mvc.perform(get("/api/anleitungen/{id}", id))
                .andExpect(jsonPath("$.titel").value("Den Notruf 112 anrufen"))
                .andExpect(jsonPath("$.schritte[2].bildPfad").value("notruf"));
    }

    // ---------- Sprache am Konto ----------

    @Test
    void dasKontoMerktSichDieSprache() throws Exception {
        Geraet geraet = neuesGeraet();
        mvc.perform(get("/api/ich").cookie(geraet.cookie())).andExpect(jsonPath("$.sprache").value("de"));

        mvc.perform(put("/api/ich/sprache").with(csrfKopf()).cookie(geraet.cookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sprache\":\"it\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/ich").cookie(geraet.cookie())).andExpect(jsonPath("$.sprache").value("it"));

        // Eine unbekannte Sprache wird zu Deutsch
        mvc.perform(put("/api/ich/sprache").with(csrfKopf()).cookie(geraet.cookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sprache\":\"xx\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/ich").cookie(geraet.cookie())).andExpect(jsonPath("$.sprache").value("de"));
    }
}
