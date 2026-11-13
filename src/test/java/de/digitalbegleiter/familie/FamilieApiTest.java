package de.digitalbegleiter.familie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;

import de.digitalbegleiter.IntegrationsTest;
import de.digitalbegleiter.gemeinsam.Schluessel;

class FamilieApiTest extends IntegrationsTest {

    private ResultActions koppelVersuch(Geraet geraet, String code) throws Exception {
        return mvc.perform(post("/api/geraete/koppeln").with(csrfKopf()).cookie(geraet.cookie())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("code", code))));
    }

    private ResultActions frage(Geraet geraet, String thema, String geteilterText) throws Exception {
        Map<String, String> anfrage = geteilterText == null
                ? Map.of("thema", thema)
                : Map.of("thema", thema, "geteilterText", geteilterText);
        return mvc.perform(post("/api/hilfeanfragen").with(csrfKopf()).cookie(geraet.cookie())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(anfrage)));
    }

    // ---------- Einladung und Koppeln ----------

    @Test
    void einladungEnthaeltCodeLinkUndQrCodeUndSpeichertNurDenHash() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");

        MvcResult ergebnis = mvc.perform(post("/api/familien/{id}/einladungen", familie).with(csrfKopf())
                        .session(anna.sitzung()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nameFuerSenior\":\"Mama\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrSvg", startsWith("<svg")))
                .andExpect(jsonPath("$.link", startsWith("https://test.example/#/verbinden/")))
                .andReturn();
        String code = json.readTree(ergebnis.getResponse().getContentAsString()).get("code").asText();

        assertThat(code).hasSize(8);
        String gespeichert = jdbc.queryForObject("SELECT code_hash FROM einladung WHERE familie_id = ?",
                String.class, familie);
        assertThat(gespeichert).isEqualTo(Schluessel.sha256(code)).isNotEqualTo(code);
    }

    @Test
    void seniorLoestCodeEinUndGehoertDannZurFamilie() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        String code = neueEinladung(anna, familie, "Mama");
        Geraet mama = neuesGeraet();

        // Abtippen mit Kleinbuchstaben, Leerzeichen und Bindestrich ist erlaubt
        String getippt = code.substring(0, 4).toLowerCase() + " - " + code.substring(4).toLowerCase();
        koppelVersuch(mama, getippt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Familie Anna"))
                .andExpect(jsonPath("$.rolle").value("SENIOR"));

        mvc.perform(get("/api/ich/familien").cookie(mama.cookie()))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/familien/{id}", familie).session(anna.sitzung()))
                .andExpect(jsonPath("$.mitglieder", hasSize(2)))
                .andExpect(jsonPath("$.mitglieder[1].anzeigename").value("Mama"))
                .andExpect(jsonPath("$.mitglieder[1].rolle").value("SENIOR"))
                .andExpect(jsonPath("$.mitglieder[1].geraete[0].id").value(mama.geraetId().toString()));
    }

    @Test
    void einCodeGiltNurEinmal() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        String code = neueEinladung(anna, familie, "Mama");

        koppelVersuch(neuesGeraet(), code).andExpect(status().isOk());
        koppelVersuch(neuesGeraet(), code).andExpect(status().isNotFound());
    }

    @Test
    void einAbgelaufenerCodeGiltNicht() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        String code = neueEinladung(anna, familie, "Mama");
        // 16 Minuten in die Vergangenheit: Die Einladung galt 15 Minuten
        jdbc.update("UPDATE einladung SET gueltig_bis = now() - interval '1 minute' WHERE familie_id = ?", familie);

        koppelVersuch(neuesGeraet(), code)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", startsWith("Dieser Code ist nicht gültig oder abgelaufen.")));
    }

    @Test
    void einFalscherCodeGiltNicht() throws Exception {
        koppelVersuch(neuesGeraet(), "FALSCH99").andExpect(status().isNotFound());
    }

    @Test
    void eineEinladungGiltFuenfzehnMinuten() {
        assertThat(FamilienService.EINLADUNG_GUELTIG).hasMinutes(15);
    }

    // ---------- Hilfeanfragen ----------

    @Test
    void ohneFamilieKannManNichtFragen() throws Exception {
        frage(neuesGeraet(), "BITTE_ANRUFEN", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sie sind noch nicht mit Ihrer Familie verbunden."));
    }

    @Test
    void seniorFragtAngehoerigerSiehtUndErledigt() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));

        frage(mama, "KOMISCHE_NACHRICHT", "Ihr Paket wartet, bitte hier klicken")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.familien").value(1));

        MvcResult liste = mvc.perform(get("/api/familien/{id}/hilfeanfragen", familie).param("status", "OFFEN")
                        .session(anna.sitzung()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vonName").value("Mama"))
                .andExpect(jsonPath("$[0].themaText").value("Ich habe eine komische Nachricht bekommen"))
                .andExpect(jsonPath("$[0].geteilterText").value("Ihr Paket wartet, bitte hier klicken"))
                .andReturn();
        JsonNode anfrage = json.readTree(liste.getResponse().getContentAsString()).get(0);

        mvc.perform(patch("/api/hilfeanfragen/{id}", anfrage.get("id").asText()).with(csrfKopf())
                        .session(anna.sitzung()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ERLEDIGT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERLEDIGT"));

        mvc.perform(get("/api/familien/{id}/hilfeanfragen", familie).param("status", "OFFEN")
                        .session(anna.sitzung()))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void derInhaltWirdNurGespeichertWennDerSeniorIhnTeilt() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));

        frage(mama, "BITTE_ANRUFEN", null).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT geteilter_text FROM hilfeanfrage WHERE familie_id = ?",
                String.class, familie)).isNull();
    }

    @Test
    void fremdeAngehoerigeKoennenEineAnfrageNichtAendern() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));
        frage(mama, "HANDY_HILFE", null).andExpect(status().isCreated());
        UUID anfrageId = jdbc.queryForObject("SELECT id FROM hilfeanfrage WHERE familie_id = ?", UUID.class, familie);

        mvc.perform(patch("/api/hilfeanfragen/{id}", anfrageId).with(csrfKopf())
                        .session(neuerAngehoeriger("Fremder").sitzung()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ERLEDIGT\"}"))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT status FROM hilfeanfrage WHERE id = ?", String.class, anfrageId))
                .isEqualTo("OFFEN");
    }

    // ---------- Gerät sperren ----------

    @Test
    void verwalterSperrtVerlorenesHandy() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));

        // Ein fremder Angehöriger darf das nicht
        mvc.perform(delete("/api/geraete/{id}", mama.geraetId()).with(csrfKopf())
                        .session(neuerAngehoeriger("Fremder").sitzung()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/ich").cookie(mama.cookie())).andExpect(status().isOk());

        mvc.perform(delete("/api/geraete/{id}", mama.geraetId()).with(csrfKopf()).session(anna.sitzung()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/ich").cookie(mama.cookie())).andExpect(status().isUnauthorized());
    }

    @Test
    void einGeraetOhneFamilieKannNiemandSperren() throws Exception {
        Geraet fremdes = neuesGeraet();
        mvc.perform(delete("/api/geraete/{id}", fremdes.geraetId()).with(csrfKopf())
                        .session(neuerAngehoeriger("Anna").sitzung()))
                .andExpect(status().isNotFound());
    }
}
