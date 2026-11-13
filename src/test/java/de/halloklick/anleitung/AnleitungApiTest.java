package de.halloklick.anleitung;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import de.halloklick.IntegrationsTest;

class AnleitungApiTest extends IntegrationsTest {

    @Test
    void sucheFindetDiePassendeAnleitungFuerDasEigeneHandy() throws Exception {
        mvc.perform(get("/api/anleitungen").param("suche", "Foto schicken").param("plattform", "ANDROID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titel").value("Ein Foto mit WhatsApp schicken"))
                .andExpect(jsonPath("$[0].plattform").value("ANDROID"))
                .andExpect(jsonPath("$[*].plattform", everyItem(oneOf("ANDROID", "ALLE"))));
    }

    @Test
    void eineGanzeFrageFindetAuchEtwas() throws Exception {
        // "Bild" steht nur in den Stichwörtern, "wie", "ich", "ein" sind Füllwörter
        mvc.perform(get("/api/anleitungen").param("suche", "wie schicke ich ein Bild").param("plattform", "IOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titel").value("Ein Foto mit WhatsApp schicken"))
                .andExpect(jsonPath("$[0].plattform").value("IOS"));
    }

    @Test
    void sucheNachEinemWortteilImTitelFunktioniert() throws Exception {
        mvc.perform(get("/api/anleitungen").param("suche", "WLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].titel", hasItem(containsString("WLAN"))));
    }

    @Test
    void unsinnFindetNichts() throws Exception {
        mvc.perform(get("/api/anleitungen").param("suche", "xyzqwertz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sqlSonderzeichenInDerSucheSindHarmlos() throws Exception {
        mvc.perform(get("/api/anleitungen").param("suche", "%")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/anleitungen").param("suche", "'; DROP TABLE anleitung; --"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/anleitungen")).andExpect(jsonPath("$.length()", greaterThan(5)));
    }

    @Test
    void ohneSuchwortKommenAlleAnleitungen() throws Exception {
        mvc.perform(get("/api/anleitungen").param("plattform", "IOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(3)))
                .andExpect(jsonPath("$[*].plattform", not(hasItem("ANDROID"))));
    }

    @Test
    void eineAnleitungHatIhreSchritteInDerRichtigenReihenfolge() throws Exception {
        MvcResult suche = mvc.perform(get("/api/anleitungen").param("suche", "Videoanruf")).andReturn();
        String id = json.readTree(suche.getResponse().getContentAsString()).get(0).get("id").asText();

        mvc.perform(get("/api/anleitungen/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quelle").value("SYSTEM"))
                .andExpect(jsonPath("$.schritte[0].nummer").value(1))
                .andExpect(jsonPath("$.schritte[1].nummer").value(2))
                .andExpect(jsonPath("$.schritte[0].text").value("Öffnen Sie WhatsApp."));
    }

    @Test
    void unbekannteAnleitungGibt404() throws Exception {
        mvc.perform(get("/api/anleitungen/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Diese Anleitung gibt es nicht."));
    }

    @Test
    void eigeneAnleitungenSiehtNurDieEigeneFamilie() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));
        Geraet fremdesGeraet = neuesGeraet();

        // Anna schreibt eine eigene Anleitung
        MvcResult erstellt = mvc.perform(post("/api/familien/{id}/anleitungen", familie).with(csrfKopf())
                        .session(anna.sitzung()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "titel", "So rufst du mich per Zauberknopf an",
                                "plattform", "ALLE",
                                "schritte", List.of("Tippe auf mein Bild.", "Tippe auf den grünen Hörer.")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quelle").value("FAMILIE"))
                .andExpect(jsonPath("$.schritte", hasSize(2)))
                .andReturn();
        String id = json.readTree(erstellt.getResponse().getContentAsString()).get("id").asText();

        // Mama findet und sieht sie
        mvc.perform(get("/api/anleitungen").param("suche", "Zauberknopf").cookie(mama.cookie()))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/anleitungen/{id}", id).cookie(mama.cookie())).andExpect(status().isOk());

        // Ein fremdes Gerät und nicht Angemeldete sehen sie nicht
        mvc.perform(get("/api/anleitungen").param("suche", "Zauberknopf").cookie(fremdesGeraet.cookie()))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/anleitungen/{id}", id).cookie(fremdesGeraet.cookie()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/anleitungen").param("suche", "Zauberknopf")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/anleitungen/{id}", id)).andExpect(status().isNotFound());

        // Ändern
        mvc.perform(put("/api/familien/{f}/anleitungen/{a}", familie, id).with(csrfKopf())
                        .session(anna.sitzung()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "titel", "So rufst du mich an",
                                "plattform", "ANDROID",
                                "schritte", List.of("Nur noch ein Schritt.")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titel").value("So rufst du mich an"))
                .andExpect(jsonPath("$.schritte", hasSize(1)));

        // Löschen
        mvc.perform(delete("/api/familien/{f}/anleitungen/{a}", familie, id).with(csrfKopf())
                        .session(anna.sitzung()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/anleitungen/{id}", id).cookie(mama.cookie())).andExpect(status().isNotFound());
    }

    @Test
    void eineAnleitungOhneSchritteWirdAbgelehnt() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        mvc.perform(post("/api/familien/{id}/anleitungen", familie).with(csrfKopf())
                        .session(anna.sitzung()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titel\":\"Leer\",\"plattform\":\"ALLE\",\"schritte\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("Bitte schreiben Sie mindestens einen Schritt.")));
    }
}
