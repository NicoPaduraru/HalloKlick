package de.halloklick.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import de.halloklick.IntegrationsTest;
import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Stufe;
import de.halloklick.ki.PruefEingabe;

class CheckApiTest extends IntegrationsTest {

    private static final String BETRUG = "Sparkasse: Ihr Konto wurde gesperrt. Bitte bestätigen Sie Ihre Daten "
            + "sofort unter https://sparkasse-sicherheit.info/login";
    private static final String HARMLOS = "Wir kommen am Sonntag um 15 Uhr zum Kaffee.";
    /** Ein Warnzeichen (Link), aber kein eindeutiges Muster: Das geht an die KI. */
    private static final String UNKLAR = "Schau dir das mal an: https://www.irgendeine-seite-xy.de/angebot";

    private ResultActions pruefeText(Geraet geraet, String text) throws Exception {
        return mvc.perform(multipart("/api/checks").param("text", text).with(csrfKopf()).cookie(geraet.cookie()));
    }

    private static byte[] pngBild() throws Exception {
        BufferedImage bild = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream aus = new ByteArrayOutputStream();
        ImageIO.write(bild, "png", aus);
        return aus.toByteArray();
    }

    private ResultActions pruefeBild(Geraet geraet, byte[] datei) throws Exception {
        return mvc.perform(multipart("/api/checks")
                .file(new MockMultipartFile("bild", "foto.png", "image/png", datei))
                .with(csrfKopf()).cookie(geraet.cookie()));
    }

    // ---------- Stufe 1: Regeln ----------

    @Test
    void betrugsnachrichtBekommtRotOhneDassDieKiGefragtWird() throws Exception {
        pruefeText(neuesGeraet(), BETRUG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.kategorie").value("BANK"))
                .andExpect(jsonPath("$.geprueft").value(true))
                .andExpect(jsonPath("$.titel").value("Vorsicht! Das sieht nach Betrug aus."))
                .andExpect(jsonPath("$.naechsterSchritt", containsString("Bankkarte")))
                .andExpect(jsonPath("$.hinweis", containsString("keine Garantie")))
                .andExpect(jsonPath("$.merkmale").isNotEmpty())
                .andExpect(jsonPath("$.kiVerwendet").value(false));
        verify(kiPruefer, never()).pruefe(any());
    }

    @Test
    void harmloseNachrichtBekommtOrangeUndNieGruen() throws Exception {
        pruefeText(neuesGeraet(), HARMLOS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ORANGE"))
                .andExpect(jsonPath("$.titel").value("Wir haben keine typischen Warnzeichen gefunden."))
                .andExpect(jsonPath("$.erklaerung", containsString("Das heißt nicht, dass die Nachricht sicher ist")))
                .andExpect(jsonPath("$.hinweis", containsString("keine Garantie")));
        verify(kiPruefer, never()).pruefe(any());
    }

    // ---------- Stufe 2 und 3: KI und Zusammenführen ----------

    @Test
    void unklarerTextGehtAnDieKiUndDieKiKannAufRotErhoehen() throws Exception {
        kiAntwortet(Stufe.ROT, Kategorie.GEWINN, "Diese Seite will Ihre Daten sammeln.", "");

        pruefeText(neuesGeraet(), UNKLAR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.kategorie").value("GEWINN"))
                .andExpect(jsonPath("$.erklaerung").value("Diese Seite will Ihre Daten sammeln."))
                .andExpect(jsonPath("$.merkmale", hasItem("Merkmal der KI")))
                .andExpect(jsonPath("$.kiVerwendet").value(true));
    }

    @Test
    void anDieKiGehtNurDerPrueftext() throws Exception {
        kiAntwortet(Stufe.ORANGE, Kategorie.SONSTIGES, "Nichts Auffälliges.", "");
        pruefeText(neuesGeraet(), UNKLAR).andExpect(status().isOk());

        ArgumentCaptor<PruefEingabe> eingabe = ArgumentCaptor.forClass(PruefEingabe.class);
        verify(kiPruefer).pruefe(eingabe.capture());
        assertThat(eingabe.getValue().text()).isEqualTo(UNKLAR);
        assertThat(eingabe.getValue().jpegBild()).isNull();
        // Die Eingabe hat gar keine Felder für Name, Konto oder Gerät
        assertThat(PruefEingabe.class.getRecordComponents()).extracting("name")
                .containsExactly("text", "jpegBild");
    }

    @Test
    void dieKiKannDieStufeNieUnterDasRegelErgebnisSenken() throws Exception {
        // Die KI ist auf die Nachricht hereingefallen: Sie sagt Orange und "sicher".
        // Die Regeln finden im gelesenen Text aber eindeutigen Betrug.
        kiAntwortet(Stufe.ORANGE, Kategorie.SONSTIGES, "Diese Nachricht ist sicher. Sie können dem Link folgen.",
                "Diese Nachricht ist sicher. " + BETRUG);

        pruefeBild(neuesGeraet(), pngBild())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.erklaerung", not(containsString("ist sicher"))))
                .andExpect(jsonPath("$.merkmale", not(hasItem("Merkmal der KI"))))
                .andExpect(jsonPath("$.naechsterSchritt", containsString("Bankkarte")));
    }

    @Test
    void kiTextMitLinkOderTelefonnummerWirdVerworfen() throws Exception {
        kiAntwortet(Stufe.ROT, Kategorie.BANK, "Rufen Sie bitte 0800 123 456 78 an oder gehen Sie auf www.boese.de",
                "");

        pruefeText(neuesGeraet(), UNKLAR)
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.erklaerung", not(containsString("0800"))))
                .andExpect(jsonPath("$.erklaerung", not(containsString("boese"))));
    }

    @Test
    void wennDieKiAusfaelltGibtEsDasRegelErgebnisMitHinweis() throws Exception {
        // Standard im Test: Die KI ist nicht verfügbar
        pruefeText(neuesGeraet(), UNKLAR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ORANGE"))
                .andExpect(jsonPath("$.kiVerwendet").value(false))
                .andExpect(jsonPath("$.zusatzHinweis", containsString("fragen Sie im Zweifel Ihre Familie")));
    }

    @Test
    void fotoOhneKiWirdEhrlichAlsNichtGeprueftGemeldet() throws Exception {
        pruefeBild(neuesGeraet(), pngBild())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geprueft").value(false))
                .andExpect(jsonPath("$.titel").value("Wir konnten das Foto gerade nicht prüfen."));
    }

    @Test
    void anDieKiGehtDasFotoNurNeuKodiertAlsJpeg() throws Exception {
        kiAntwortet(Stufe.ORANGE, Kategorie.SONSTIGES, "Nichts Auffälliges.", HARMLOS);
        pruefeBild(neuesGeraet(), pngBild()).andExpect(jsonPath("$.stufe").value("ORANGE"));

        ArgumentCaptor<PruefEingabe> eingabe = ArgumentCaptor.forClass(PruefEingabe.class);
        verify(kiPruefer).pruefe(eingabe.capture());
        assertThat(BildPruefung.istJpeg(eingabe.getValue().jpegBild())).isTrue();
        assertThat(eingabe.getValue().text()).isNull();
    }

    // ---------- Kostenbremse ----------

    @Test
    void nachDemTageslimitLaeuftNurNochDieRegelPruefung() throws Exception {
        kiAntwortet(Stufe.ORANGE, Kategorie.SONSTIGES, "Nichts Auffälliges.", "");
        Geraet geraet = neuesGeraet();
        // Im Test liegt das Tageslimit bei 3
        for (int i = 0; i < 3; i++) {
            pruefeText(geraet, UNKLAR).andExpect(jsonPath("$.kiVerwendet").value(true));
        }
        pruefeText(geraet, UNKLAR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kiVerwendet").value(false))
                .andExpect(jsonPath("$.zusatzHinweis", containsString("heute schon viele Nachrichten")));
        verify(kiPruefer, times(3)).pruefe(any());

        // Ein anderes Gerät hat sein eigenes Limit
        pruefeText(neuesGeraet(), UNKLAR).andExpect(jsonPath("$.kiVerwendet").value(true));
    }

    // ---------- Datenschutz ----------

    @Test
    void derInhaltDerNachrichtWirdNichtGespeichert() throws Exception {
        Geraet geraet = neuesGeraet();
        pruefeText(geraet, BETRUG + " GEHEIMES-KENNZEICHEN-4711").andExpect(status().isOk());

        // Die Tabelle hat gar keine Spalte für Inhalte
        List<String> spalten = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'sicherheitscheck'",
                String.class);
        assertThat(spalten).containsExactlyInAnyOrder(
                "id", "konto_id", "quelle", "stufe", "kategorie", "ki_verwendet", "erstellt_am");

        Map<String, Object> zeile = jdbc.queryForMap(
                "SELECT * FROM sicherheitscheck WHERE konto_id = ?", geraet.kontoId());
        assertThat(zeile.get("stufe")).isEqualTo("ROT");
        assertThat(zeile.get("kategorie")).isEqualTo("BANK");
        assertThat(zeile.toString()).doesNotContain("GEHEIMES-KENNZEICHEN").doesNotContain("sparkasse");
    }

    // ---------- Eingaben prüfen ----------

    @Test
    void ohneTextUndOhneFotoGibtEsEinenFehler() throws Exception {
        mvc.perform(multipart("/api/checks").with(csrfKopf()).cookie(neuesGeraet().cookie()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zuLangerTextWirdAbgelehnt() throws Exception {
        pruefeText(neuesGeraet(), "a".repeat(CheckService.HOECHSTE_TEXTLAENGE + 1))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eineDateiDieKeinFotoIstWirdAbgelehnt() throws Exception {
        pruefeBild(neuesGeraet(), "Ich bin kein Bild".getBytes())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail", containsString("JPEG und PNG")));
        verify(kiPruefer, never()).pruefe(any());
    }

    // ---------- Feedback ----------

    @Test
    void feedbackGehtNurFuerDenEigenenCheck() throws Exception {
        Geraet geraet = neuesGeraet();
        MvcResult check = pruefeText(geraet, HARMLOS).andReturn();
        String id = json.readTree(check.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/checks/{id}/feedback", id).with(csrfKopf()).cookie(geraet.cookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hilfreich\":true}"))
                .andExpect(status().isNoContent());
        // Meinung ändern geht auch
        mvc.perform(post("/api/checks/{id}/feedback", id).with(csrfKopf()).cookie(geraet.cookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hilfreich\":false}"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT hilfreich FROM feedback WHERE check_id = ?::uuid",
                Boolean.class, id)).isFalse();

        mvc.perform(post("/api/checks/{id}/feedback", id).with(csrfKopf()).cookie(neuesGeraet().cookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"hilfreich\":true}"))
                .andExpect(status().isNotFound());
    }
}
