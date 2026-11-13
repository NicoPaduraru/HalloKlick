package de.digitalbegleiter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalbegleiter.gemeinsam.Kategorie;
import de.digitalbegleiter.gemeinsam.Stufe;
import de.digitalbegleiter.gemeinsam.sicherheit.GeraeteCookie;
import de.digitalbegleiter.ki.KiEinschaetzung;
import de.digitalbegleiter.ki.KiErgebnis;
import de.digitalbegleiter.ki.KiNichtVerfuegbarException;
import de.digitalbegleiter.ki.NachrichtenPruefer;
import de.digitalbegleiter.ki.NaechsterSchritt;
import jakarta.servlet.http.Cookie;

/**
 * Basis für alle Tests, die die ganze Anwendung mit echter Datenbank starten.
 * Die KI ist durch ein Test-Double ersetzt: Es wird nie die echte Claude API aufgerufen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationsTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Test-Double für die KI. Ohne weitere Angabe meldet es "nicht verfügbar". */
    @MockitoBean
    protected NachrichtenPruefer kiPruefer;

    @BeforeEach
    void kiIstStandardmaessigNichtVerfuegbar() {
        reset(kiPruefer);
        when(kiPruefer.pruefe(any()))
                .thenThrow(new KiNichtVerfuegbarException(KiErgebnis.Grund.FEHLER, "Test: KI nicht verfügbar"));
    }

    /** Ein gültiges CSRF-Token mitschicken (die Test-Bibliothek von Spring Security erzeugt es). */
    protected static RequestPostProcessor csrfKopf() {
        return csrf();
    }

    protected void kiAntwortet(Stufe stufe, Kategorie kategorie, String erklaerung, String erkannterText) {
        reset(kiPruefer);
        when(kiPruefer.pruefe(any())).thenReturn(new KiEinschaetzung(stufe, kategorie, List.of("Merkmal der KI"),
                erklaerung, NaechsterSchritt.FAMILIE_FRAGEN, erkannterText, 800, 120));
    }

    /** Ein Senioren-Gerät: das Cookie mit dem Geräte-Schlüssel und die Konto-Nummer. */
    protected record Geraet(Cookie cookie, UUID kontoId, UUID geraetId) {
    }

    protected Geraet neuesGeraet() throws Exception {
        MvcResult ergebnis = mvc.perform(post("/api/geraete").with(csrfKopf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plattform\":\"ANDROID\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode antwort = json.readTree(ergebnis.getResponse().getContentAsString());
        Cookie cookie = ergebnis.getResponse().getCookie(GeraeteCookie.NAME);
        return new Geraet(cookie, UUID.fromString(antwort.get("kontoId").asText()),
                UUID.fromString(antwort.get("geraetId").asText()));
    }

    /** Ein angemeldeter Angehöriger: seine Sitzung und die Konto-Nummer. */
    protected record Angehoeriger(MockHttpSession sitzung, UUID kontoId, String email) {
    }

    protected static final String TEST_PASSWORT = "ein-langes-test-passwort";

    protected Angehoeriger neuerAngehoeriger(String name) throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.org";
        MvcResult ergebnis = mvc.perform(post("/api/auth/registrieren").with(csrfKopf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "passwort", TEST_PASSWORT, "anzeigename", name))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode antwort = json.readTree(ergebnis.getResponse().getContentAsString());
        return new Angehoeriger((MockHttpSession) ergebnis.getRequest().getSession(false),
                UUID.fromString(antwort.get("id").asText()), email);
    }

    protected UUID neueFamilie(Angehoeriger angehoeriger, String name) throws Exception {
        MvcResult ergebnis = mvc.perform(post("/api/familien").with(csrfKopf()).session(angehoeriger.sitzung())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(ergebnis.getResponse().getContentAsString()).get("id").asText());
    }

    /** Erzeugt eine Einladung und gibt den Code zurück. */
    protected String neueEinladung(Angehoeriger angehoeriger, UUID familieId, String nameFuerSenior)
            throws Exception {
        MvcResult ergebnis = mvc.perform(post("/api/familien/{id}/einladungen", familieId).with(csrfKopf())
                        .session(angehoeriger.sitzung())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nameFuerSenior", nameFuerSenior))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(ergebnis.getResponse().getContentAsString()).get("code").asText();
    }

    protected void koppeln(Geraet geraet, String code) throws Exception {
        mvc.perform(post("/api/geraete/koppeln").with(csrfKopf()).cookie(geraet.cookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("code", code))))
                .andExpect(status().isOk());
    }
}
