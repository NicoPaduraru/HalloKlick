package de.digitalbegleiter.gemeinsam.sicherheit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalbegleiter.gemeinsam.Einstellungen;

class AnfrageBegrenzungTest {

    private final AtomicReference<Instant> jetzt = new AtomicReference<>(Instant.parse("2026-11-12T10:00:00Z"));

    /** Eine Uhr, die der Test selbst vorstellen kann. */
    private final Clock uhr = new Clock() {
        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return jetzt.get();
        }
    };

    private final AnfrageBegrenzung begrenzung = new AnfrageBegrenzung(
            new Einstellungen(null, null, null, null, null, null, null, new Einstellungen.Begrenzung(5, 2)),
            uhr, new ObjectMapper());

    private int schicke(String methode, String pfad, String adresse) throws Exception {
        MockHttpServletRequest anfrage = new MockHttpServletRequest(methode, pfad);
        anfrage.setRemoteAddr(adresse);
        MockHttpServletResponse antwort = new MockHttpServletResponse();
        begrenzung.doFilter(anfrage, antwort, new MockFilterChain());
        return antwort.getStatus();
    }

    @Test
    void heikleWegeHabenEineStrengeGrenze() throws Exception {
        assertThat(schicke("POST", "/api/auth/anmelden", "1.1.1.1")).isEqualTo(200);
        assertThat(schicke("POST", "/api/auth/anmelden", "1.1.1.1")).isEqualTo(200);
        assertThat(schicke("POST", "/api/auth/anmelden", "1.1.1.1")).isEqualTo(429);
        // Koppeln zählt in denselben Topf: Einladungscodes lassen sich nicht durchprobieren
        assertThat(schicke("POST", "/api/geraete/koppeln", "1.1.1.1")).isEqualTo(429);
    }

    @Test
    void jedeAdresseHatIhrenEigenenZaehler() throws Exception {
        schicke("POST", "/api/checks", "1.1.1.1");
        schicke("POST", "/api/checks", "1.1.1.1");
        assertThat(schicke("POST", "/api/checks", "1.1.1.1")).isEqualTo(429);
        assertThat(schicke("POST", "/api/checks", "2.2.2.2")).isEqualTo(200);
    }

    @Test
    void nachEinerMinuteGehtEsWieder() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(schicke("GET", "/api/anleitungen", "1.1.1.1")).isEqualTo(200);
        }
        assertThat(schicke("GET", "/api/anleitungen", "1.1.1.1")).isEqualTo(429);

        jetzt.set(jetzt.get().plusSeconds(61));

        assertThat(schicke("GET", "/api/anleitungen", "1.1.1.1")).isEqualTo(200);
    }

    @Test
    void dieDateienDerWebAppWerdenNichtGezaehlt() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(schicke("GET", "/app.js", "1.1.1.1")).isEqualTo(200);
        }
    }
}
