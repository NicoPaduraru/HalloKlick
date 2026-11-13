package de.halloklick.ki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.halloklick.IntegrationsTest;
import de.halloklick.gemeinsam.Einstellungen;
import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Stufe;

/** Die Kostenbremse für alle zusammen: Monatsbudget und Kosten-Übersicht. */
class KostenbremseTest extends IntegrationsTest {

    @Autowired
    private NutzungRepository nutzung;

    @Autowired
    private Einstellungen einstellungen;

    private final LocalDate heute = LocalDate.now(ZoneId.of("Europe/Berlin"));

    @BeforeEach
    @AfterEach
    void verbrauchZuruecksetzen() {
        jdbc.update("DELETE FROM ki_tagesverbrauch");
    }

    /** Ein KiDienst mit eigenen KI-Einstellungen, der Rest kommt aus der Test-Konfiguration. */
    private KiDienst dienstMit(boolean aktiv, String apiKey, String budgetUsd) {
        Einstellungen.Ki ki = new Einstellungen.Ki(aktiv, apiKey, "claude-opus-5-5", 10, new BigDecimal(budgetUsd),
                new BigDecimal("4.00"), new BigDecimal("20.00"));
        Einstellungen eigene = new Einstellungen(einstellungen.basisUrl(), einstellungen.internToken(),
                einstellungen.cookies(), einstellungen.funktionen(), ki, einstellungen.push(),
                einstellungen.mail(), einstellungen.begrenzung());
        return new KiDienst(kiPruefer, nutzung, eigene, Clock.systemUTC());
    }

    @Test
    void istDasMonatsbudgetErreichtWirdDieKiNichtMehrGefragt() throws Exception {
        Geraet geraet = neuesGeraet();
        KiDienst dienst = dienstMit(true, "schluessel", "1.00");
        // 100.000 Ausgabe-Tokens zu 20 $ pro Million = 2 $, also über dem Budget von 1 $
        nutzung.verbuche(heute, 0, 100_000);

        KiErgebnis ergebnis = dienst.pruefe(geraet.kontoId(), PruefEingabe.ausText("Hallo"));

        assertThat(ergebnis.grund()).isEqualTo(KiErgebnis.Grund.MONATSBUDGET);
        assertThat(ergebnis.einschaetzung()).isNull();
        verify(kiPruefer, never()).pruefe(any());
    }

    @Test
    void unterDemBudgetWirdGefragtUndDerVerbrauchGezaehlt() throws Exception {
        Geraet geraet = neuesGeraet();
        kiAntwortet(Stufe.ORANGE, Kategorie.SONSTIGES, "Nichts Auffälliges.", "");
        KiDienst dienst = dienstMit(true, "schluessel", "1.00");

        KiErgebnis ergebnis = dienst.pruefe(geraet.kontoId(), PruefEingabe.ausText("Hallo"));

        assertThat(ergebnis.grund()).isEqualTo(KiErgebnis.Grund.OK);
        KiDienst.Uebersicht uebersicht = dienst.uebersicht();
        assertThat(uebersicht.tage()).hasSize(1);
        assertThat(uebersicht.tage().get(0).aufrufe()).isEqualTo(1);
        // Das Test-Double meldet 800 Eingabe- und 120 Ausgabe-Tokens: 800*4/1M + 120*20/1M = 0,0056 $
        assertThat(uebersicht.kostenDiesenMonatUsd()).isEqualByComparingTo("0.0056");
    }

    @Test
    void ohneSchluesselOderOhneBudgetBleibtDieKiAus() throws Exception {
        Geraet geraet = neuesGeraet();

        assertThat(dienstMit(false, "schluessel", "5").pruefe(geraet.kontoId(), PruefEingabe.ausText("x")).grund())
                .isEqualTo(KiErgebnis.Grund.ABGESCHALTET);
        assertThat(dienstMit(true, "", "5").pruefe(geraet.kontoId(), PruefEingabe.ausText("x")).grund())
                .isEqualTo(KiErgebnis.Grund.ABGESCHALTET);
        assertThat(dienstMit(true, "schluessel", "0").pruefe(geraet.kontoId(), PruefEingabe.ausText("x")).grund())
                .isEqualTo(KiErgebnis.Grund.ABGESCHALTET);
        verify(kiPruefer, never()).pruefe(any());
    }

    @Test
    void einUnerwarteterFehlerDerKiWirdAbgefangen() throws Exception {
        Geraet geraet = neuesGeraet();
        org.mockito.Mockito.reset(kiPruefer);
        org.mockito.Mockito.when(kiPruefer.pruefe(any())).thenThrow(new IllegalStateException("kaputt"));

        KiErgebnis ergebnis = dienstMit(true, "schluessel", "5").pruefe(geraet.kontoId(), PruefEingabe.ausText("x"));

        assertThat(ergebnis.grund()).isEqualTo(KiErgebnis.Grund.FEHLER);
    }
}
