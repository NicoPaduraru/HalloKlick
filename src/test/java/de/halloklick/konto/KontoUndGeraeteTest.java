package de.halloklick.konto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import de.halloklick.IntegrationsTest;
import de.halloklick.gemeinsam.Schluessel;
import de.halloklick.gemeinsam.sicherheit.GeraeteCookie;
import jakarta.servlet.http.Cookie;

class KontoUndGeraeteTest extends IntegrationsTest {

    // ---------- Geräte (Senioren) ----------

    @Test
    void einNeuesGeraetBekommtSeinenSchluesselNurAlsHttpOnlyCookie() throws Exception {
        MvcResult ergebnis = mvc.perform(post("/api/geraete").with(csrfKopf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kontoId").exists())
                .andReturn();

        Cookie cookie = ergebnis.getResponse().getCookie(GeraeteCookie.NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getValue()).hasSizeGreaterThanOrEqualTo(40);
        // Der Schlüssel steht nicht im Antworttext, JavaScript kommt also nie an ihn heran
        assertThat(ergebnis.getResponse().getContentAsString()).doesNotContain(cookie.getValue());
    }

    @Test
    void inDerDatenbankLiegtNurDerHashDesSchluessels() throws Exception {
        Geraet geraet = neuesGeraet();
        String gespeichert = jdbc.queryForObject("SELECT token_hash FROM geraet WHERE id = ?", String.class,
                geraet.geraetId());

        assertThat(gespeichert).isNotEqualTo(geraet.cookie().getValue());
        assertThat(gespeichert).isEqualTo(Schluessel.sha256(geraet.cookie().getValue()));
    }

    @Test
    void einGeraetIstAlsSeniorAngemeldetUndBrauchtKeineFamilie() throws Exception {
        Geraet geraet = neuesGeraet();
        mvc.perform(get("/api/ich").cookie(geraet.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typ").value("SENIOR"))
                .andExpect(jsonPath("$.tarif").value("KOSTENLOS"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void werSchonEinGeraetHatBekommtKeinZweites() throws Exception {
        Geraet geraet = neuesGeraet();
        mvc.perform(post("/api/geraete").with(csrfKopf()).cookie(geraet.cookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.geraetId").value(geraet.geraetId().toString()));
    }

    @Test
    void einErfundenerSchluesselMeldetNiemandenAn() throws Exception {
        mvc.perform(get("/api/ich").cookie(new Cookie(GeraeteCookie.NAME, "erfunden-erfunden-erfunden")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void einGesperrtesGeraetIstNichtMehrAngemeldet() throws Exception {
        Geraet geraet = neuesGeraet();
        jdbc.update("UPDATE geraet SET widerrufen = true WHERE id = ?", geraet.geraetId());

        mvc.perform(get("/api/ich").cookie(geraet.cookie())).andExpect(status().isUnauthorized());
    }

    // ---------- Angehörige ----------

    @Test
    void registrierenMeldetAnUndSpeichertDasPasswortNurAlsHash() throws Exception {
        Angehoeriger angehoeriger = neuerAngehoeriger("Tochter Anna");

        mvc.perform(get("/api/ich").session(angehoeriger.sitzung()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typ").value("ANGEHOERIGER"))
                .andExpect(jsonPath("$.anzeigename").value("Tochter Anna"));

        String hash = jdbc.queryForObject("SELECT passwort_hash FROM konto WHERE id = ?", String.class,
                angehoeriger.kontoId());
        assertThat(hash).startsWith("$2").doesNotContain(TEST_PASSWORT);
    }

    @Test
    void dieselbeEmailKannNichtZweimalRegistriertWerden() throws Exception {
        Angehoeriger angehoeriger = neuerAngehoeriger("Erster");
        mvc.perform(post("/api/auth/registrieren").with(csrfKopf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", angehoeriger.email().toUpperCase(),
                                "passwort", TEST_PASSWORT, "anzeigename", "Zweiter"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void einZuKurzesPasswortWirdAbgelehnt() throws Exception {
        mvc.perform(post("/api/auth/registrieren").with(csrfKopf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "kurz@example.org",
                                "passwort", "kurz", "anzeigename", "Kurz"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Das Passwort muss zwischen 10 und 72 Zeichen lang sein."));
    }

    @Test
    void anmeldenMitRichtigemPasswortUndDanachAbmelden() throws Exception {
        Angehoeriger angehoeriger = neuerAngehoeriger("Sohn Ben");

        MvcResult anmeldung = mvc.perform(post("/api/auth/anmelden").with(csrfKopf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", angehoeriger.email(), "passwort", TEST_PASSWORT))))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession sitzung = (MockHttpSession) anmeldung.getRequest().getSession(false);

        mvc.perform(get("/api/ich").session(sitzung)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/abmelden").with(csrfKopf()).session(sitzung))
                .andExpect(status().isNoContent());
        assertThat(sitzung.isInvalid()).isTrue();
    }

    @Test
    void falschesPasswortUndUnbekannteEmailBekommenDieselbeAntwort() throws Exception {
        Angehoeriger angehoeriger = neuerAngehoeriger("Sohn Carl");
        String erwartet = "E-Mail-Adresse oder Passwort stimmen nicht.";

        mvc.perform(post("/api/auth/anmelden").with(csrfKopf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", angehoeriger.email(), "passwort", "falsches-passwort"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(erwartet));

        mvc.perform(post("/api/auth/anmelden").with(csrfKopf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "gibt-es-nicht@example.org", "passwort", "falsches-passwort"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(erwartet));
    }

    // ---------- CSRF ----------

    @Test
    void aenderndeAnfragenOhneCsrfTokenWerdenAbgelehnt() throws Exception {
        mvc.perform(post("/api/geraete")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/anmelden").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.org\",\"passwort\":\"egal-egal-egal\"}"))
                .andExpect(status().isForbidden());
    }
}
