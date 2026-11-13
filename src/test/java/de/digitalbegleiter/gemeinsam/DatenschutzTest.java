package de.digitalbegleiter.gemeinsam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import de.digitalbegleiter.IntegrationsTest;
import de.digitalbegleiter.familie.FamilienService;
import de.digitalbegleiter.familie.HilfeanfrageService;
import de.digitalbegleiter.konto.GeraeteService;

/** Datenauskunft, Konto löschen und automatische Löschfristen (Bauplan, Schritt 6.1). */
class DatenschutzTest extends IntegrationsTest {

    @Autowired
    private HilfeanfrageService hilfeanfragen;

    @Autowired
    private GeraeteService geraete;

    @Autowired
    private FamilienService familien;

    private int anzahl(String tabelle, String spalte, UUID wert) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabelle + " WHERE " + spalte + " = ?",
                Integer.class, wert);
    }

    private void frage(Geraet geraet) throws Exception {
        mvc.perform(post("/api/hilfeanfragen").with(csrfKopf()).cookie(geraet.cookie())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"thema\":\"BITTE_ANRUFEN\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void datenauskunftZeigtAllesWasGespeichertIst() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));
        mvc.perform(multipart("/api/checks").param("text", "Hallo, wie geht es dir?").with(csrfKopf())
                .cookie(mama.cookie())).andExpect(status().isOk());
        frage(mama);

        mvc.perform(get("/api/ich/daten").cookie(mama.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.konto.typ").value("SENIOR"))
                .andExpect(jsonPath("$.konto.anzeigename").value("Mama"))
                .andExpect(jsonPath("$.geraete", hasSize(1)))
                .andExpect(jsonPath("$.sicherheitschecks", hasSize(1)))
                .andExpect(jsonPath("$.sicherheitschecks[0].stufe").value("ORANGE"))
                .andExpect(jsonPath("$.hilfeanfragen", hasSize(1)))
                .andExpect(jsonPath("$.familien[0].familie").value("Familie Anna"))
                .andExpect(jsonPath("$.kiNutzung").isArray())
                .andExpect(jsonPath("$.benachrichtigungen").isArray());

        mvc.perform(get("/api/ich/daten").session(anna.sitzung()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.konto.email").value(anna.email()))
                // Der Passwort-Hash gehört nicht in die Auskunft
                .andExpect(jsonPath("$.konto.passwortHash").doesNotExist());
    }

    @Test
    void seniorLoeschtSeinKontoMitAllenDaten() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));
        mvc.perform(multipart("/api/checks").param("text", "Hallo").with(csrfKopf()).cookie(mama.cookie()))
                .andExpect(status().isOk());
        frage(mama);

        mvc.perform(delete("/api/ich").with(csrfKopf()).cookie(mama.cookie())).andExpect(status().isNoContent());

        assertThat(anzahl("konto", "id", mama.kontoId())).isZero();
        assertThat(anzahl("geraet", "konto_id", mama.kontoId())).isZero();
        assertThat(anzahl("sicherheitscheck", "konto_id", mama.kontoId())).isZero();
        assertThat(anzahl("hilfeanfrage", "von_konto_id", mama.kontoId())).isZero();
        assertThat(anzahl("mitgliedschaft", "konto_id", mama.kontoId())).isZero();
        mvc.perform(get("/api/ich").cookie(mama.cookie())).andExpect(status().isUnauthorized());
        // Die Familie und das Konto der Tochter bleiben
        assertThat(anzahl("familie", "id", familie)).isOne();
        assertThat(anzahl("konto", "id", anna.kontoId())).isOne();
    }

    @Test
    void loeschtDerLetzteAngehoerigeSeinKontoVerschwindetAuchDieFamilie() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));
        frage(mama);
        mvc.perform(post("/api/familien/{id}/anleitungen", familie).with(csrfKopf()).session(anna.sitzung())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titel\":\"Eigene\",\"plattform\":\"ALLE\",\"schritte\":[\"Eins\"]}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/ich").with(csrfKopf()).session(anna.sitzung())).andExpect(status().isNoContent());

        assertThat(anzahl("konto", "id", anna.kontoId())).isZero();
        assertThat(anzahl("familie", "id", familie)).isZero();
        assertThat(anzahl("hilfeanfrage", "familie_id", familie)).isZero();
        assertThat(anzahl("anleitung", "familie_id", familie)).isZero();
        assertThat(anzahl("einladung", "familie_id", familie)).isZero();
        // Das Konto der Mutter bleibt, sie ist nur nicht mehr verbunden
        assertThat(anzahl("konto", "id", mama.kontoId())).isOne();
        assertThat(anzahl("mitgliedschaft", "konto_id", mama.kontoId())).isZero();
        mvc.perform(get("/api/ich").session(anna.sitzung())).andExpect(status().isUnauthorized());
    }

    // ---------- Löschfristen ----------

    @Test
    void hilfeanfragenWerdenNach30TagenGeloescht() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        Geraet mama = neuesGeraet();
        koppeln(mama, neueEinladung(anna, familie, "Mama"));
        frage(mama);
        frage(mama);
        // Eine der beiden Anfragen ist 31 Tage alt, die andere 29
        jdbc.update("""
                UPDATE hilfeanfrage SET erstellt_am = now() - interval '29 days' WHERE familie_id = ?
                """, familie);
        jdbc.update("""
                UPDATE hilfeanfrage SET erstellt_am = now() - interval '31 days'
                WHERE id = (SELECT id FROM hilfeanfrage WHERE familie_id = ? LIMIT 1)
                """, familie);

        hilfeanfragen.loescheAlte(Instant.now());

        assertThat(anzahl("hilfeanfrage", "familie_id", familie)).isOne();
    }

    @Test
    void inaktiveGeraeteWerdenNach12MonatenGeloescht() throws Exception {
        Geraet lange = neuesGeraet();
        Geraet kurz = neuesGeraet();
        jdbc.update("UPDATE geraet SET zuletzt_gesehen = now() - interval '366 days' WHERE id = ?", lange.geraetId());
        jdbc.update("UPDATE geraet SET zuletzt_gesehen = now() - interval '300 days' WHERE id = ?", kurz.geraetId());

        geraete.loescheInaktive(Instant.now());

        assertThat(anzahl("konto", "id", lange.kontoId())).isZero();
        assertThat(anzahl("geraet", "id", lange.geraetId())).isZero();
        assertThat(anzahl("konto", "id", kurz.kontoId())).isOne();
    }

    @Test
    void abgelaufeneEinladungenWerdenAufgeraeumt() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        UUID familie = neueFamilie(anna, "Familie Anna");
        neueEinladung(anna, familie, "Mama");
        neueEinladung(anna, familie, "Papa");
        jdbc.update("""
                UPDATE einladung SET gueltig_bis = now() - interval '2 days'
                WHERE id = (SELECT id FROM einladung WHERE familie_id = ? LIMIT 1)
                """, familie);

        familien.loescheAbgelaufeneEinladungen(Instant.now());

        assertThat(anzahl("einladung", "familie_id", familie)).isOne();
    }

    // ---------- Web Push ----------

    @Test
    void pushAdressenWerdenNurFuerEchtePushDiensteAngenommen() throws Exception {
        Angehoeriger anna = neuerAngehoeriger("Anna");
        String boese = "{\"endpoint\":\"https://interner-server.example/geheim\",\"p256dh\":\"abc\",\"auth\":\"def\"}";
        String echt = "{\"endpoint\":\"https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID()
                + "\",\"p256dh\":\"abc\",\"auth\":\"def\"}";

        mvc.perform(post("/api/push-abos").with(csrfKopf()).session(anna.sitzung())
                .contentType(MediaType.APPLICATION_JSON).content(boese)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/push-abos").with(csrfKopf()).session(anna.sitzung())
                .contentType(MediaType.APPLICATION_JSON).content(echt)).andExpect(status().isNoContent());

        assertThat(anzahl("push_abo", "konto_id", anna.kontoId())).isOne();
    }
}
