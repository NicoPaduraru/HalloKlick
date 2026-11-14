package de.halloklick.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import de.halloklick.IntegrationsTest;

/** Der feste Tipp bei "keine Warnzeichen" und das Teilen an HalloKlick. */
class TeilenUndTippTest extends IntegrationsTest {

    private static final String TIPP = "Gibt jemand vor, Polizei oder Bank zu sein? Legen Sie auf und rufen Sie 110 an.";

    @Test
    void beiKeineWarnzeichenStehtImmerDerTipp() throws Exception {
        mvc.perform(multipart("/api/checks").param("text", "Wir kommen am Sonntag zum Kaffee.").with(csrfKopf())
                        .cookie(neuesGeraet().cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stufe").value("ORANGE"))
                .andExpect(jsonPath("$.tipp").value(TIPP));
    }

    @Test
    void derTippIstUebersetzt() throws Exception {
        mvc.perform(multipart("/api/checks").param("text", "See you on Sunday.").with(csrfKopf())
                        .cookie(neuesGeraet().cookie()).header("Accept-Language", "en"))
                .andExpect(jsonPath("$.tipp").value(
                        "Is someone claiming to be the police or your bank? Hang up and call 110, the police in Germany."));
    }

    @Test
    void beiRotGibtEsDenTippNicht() throws Exception {
        mvc.perform(multipart("/api/checks").param("text", "Hallo Mama, das ist meine neue Nummer.")
                        .with(csrfKopf()).cookie(neuesGeraet().cookie()))
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.tipp").doesNotExist());
    }

    @Test
    void dasNeueWarnzeichenFalschePolizeiKommtBeimNutzerAn() throws Exception {
        mvc.perform(multipart("/api/checks")
                        .param("text", "Hier ist die Polizei. Bitte legen Sie Bargeld und Schmuck bereit, ein Kollege holt es ab.")
                        .with(csrfKopf()).cookie(neuesGeraet().cookie()))
                .andExpect(jsonPath("$.stufe").value("ROT"))
                .andExpect(jsonPath("$.kategorie").value("BEHOERDE"))
                .andExpect(jsonPath("$.merkmale[?(@ =~ /.*holt niemals Geld oder Schmuck ab.*/)]").exists());
    }

    // ---------- Teilen an HalloKlick ----------

    @Test
    void dasManifestMeldetHalloKlickAlsZielZumTeilen() throws Exception {
        try (InputStream datei = getClass().getResourceAsStream("/static/manifest.json")) {
            JsonNode teilen = json.readTree(datei).get("share_target");
            assertThat(teilen.get("action").asText()).isEqualTo("/teilen");
            // POST: Der geteilte Text steht dann nicht in der Adresse und nicht in Protokollen
            assertThat(teilen.get("method").asText()).isEqualTo("POST");
            assertThat(teilen.get("params").get("text").asText()).isEqualTo("text");
        }
    }

    @Test
    void derServerLeitetGeteiltesNurWeiterUndBrauchtKeinToken() throws Exception {
        // Ohne CSRF-Token und ohne Anmeldung: So kommt die Anfrage vom Handy
        mvc.perform(multipart("/teilen").param("text", "Geteilte Nachricht GEHEIM-4711"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/#/pruefen"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM sicherheitscheck WHERE erstellt_am > now() - interval '2 seconds'"
                + " AND konto_id IS NULL", Integer.class)).isZero();
    }
}
