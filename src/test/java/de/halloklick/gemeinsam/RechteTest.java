package de.halloklick.gemeinsam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import de.halloklick.IntegrationsTest;

/** Wer darf was? (Bauplan, Schritt 2.4) */
class RechteTest extends IntegrationsTest {

    private static final String JSON_LEER = "{}";

    @Test
    void ohneAnmeldungGehenNurDieOffenenWege() throws Exception {
        mvc.perform(get("/api/anleitungen")).andExpect(status().isOk());
        mvc.perform(get("/api/einstellungen")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());

        mvc.perform(get("/api/ich")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Bitte melden Sie sich an."));
        mvc.perform(get("/api/ich/daten")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/familien")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/checks").param("text", "Hallo").with(csrfKopf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/hilfeanfragen").with(csrfKopf()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON_LEER)).andExpect(status().isUnauthorized());
    }

    @Test
    void seniorenDuerfenNichtInDenBereichDerAngehoerigen() throws Exception {
        Geraet senior = neuesGeraet();
        UUID irgendeine = UUID.randomUUID();

        mvc.perform(get("/api/familien").cookie(senior.cookie())).andExpect(status().isForbidden());
        mvc.perform(post("/api/familien").with(csrfKopf()).cookie(senior.cookie())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Meine\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/familien/{id}/hilfeanfragen", irgendeine).cookie(senior.cookie()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/hilfeanfragen/{id}", irgendeine).with(csrfKopf()).cookie(senior.cookie())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ERLEDIGT\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/geraete/{id}", irgendeine).with(csrfKopf()).cookie(senior.cookie()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/push-abos").with(csrfKopf()).cookie(senior.cookie())
                .contentType(MediaType.APPLICATION_JSON).content(JSON_LEER))
                .andExpect(status().isForbidden());
    }

    @Test
    void angehoerigeDuerfenNichtAlsSeniorHandeln() throws Exception {
        Angehoeriger angehoeriger = neuerAngehoeriger("Anna");

        mvc.perform(post("/api/hilfeanfragen").with(csrfKopf()).session(angehoeriger.sitzung())
                .contentType(MediaType.APPLICATION_JSON).content("{\"thema\":\"BITTE_ANRUFEN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/geraete/koppeln").with(csrfKopf()).session(angehoeriger.sitzung())
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABCDEFGH\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void fremdeFamilienSindUnsichtbar() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        Angehoeriger fremder = neuerAngehoeriger("Fremder");
        UUID familieVonAnna = neueFamilie(anna, "Familie Anna");

        // Für Fremde sieht es so aus, als gäbe es die Familie nicht (404, nicht 403)
        mvc.perform(get("/api/familien/{id}", familieVonAnna).session(fremder.sitzung()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/familien/{id}/hilfeanfragen", familieVonAnna).session(fremder.sitzung()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/familien/{id}/anleitungen", familieVonAnna).session(fremder.sitzung()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/familien/{id}/einladungen", familieVonAnna).with(csrfKopf())
                .session(fremder.sitzung()).contentType(MediaType.APPLICATION_JSON).content(JSON_LEER))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/familien/{id}/anleitungen", familieVonAnna).with(csrfKopf())
                .session(fremder.sitzung()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"titel\":\"X\",\"plattform\":\"ALLE\",\"schritte\":[\"Eins\"]}"))
                .andExpect(status().isNotFound());

        // Anna selbst sieht ihre Familie
        mvc.perform(get("/api/familien/{id}", familieVonAnna).session(anna.sitzung()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meineRolle").value("VERWALTER"));
    }

    @Test
    void interneSeitenSindGeschuetzt() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/intern/ki-nutzung")).andExpect(status().isNotFound());
        mvc.perform(get("/api/intern/ki-nutzung").header("X-Intern-Token", "falsch-falsch-falsch-falsch"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/intern/ki-nutzung").header("X-Intern-Token", "test-intern-schluessel-1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modell").exists());
    }

    @Test
    void jedeAntwortTraegtDieSicherheitsKopfzeilen() throws Exception {
        mvc.perform(get("/api/einstellungen"))
                .andExpect(header().string("Content-Security-Policy", Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy",
                        Matchers.containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void dieOeffentlichenEinstellungenVerratenKeineGeheimnisse() throws Exception {
        mvc.perform(get("/api/einstellungen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sicherheitscheck").value(true))
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.internToken").doesNotExist());
    }
}
