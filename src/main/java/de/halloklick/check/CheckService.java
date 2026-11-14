package de.halloklick.check;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.halloklick.check.Sicherheitscheck.Quelle;
import de.halloklick.check.regel.Merkmal;
import de.halloklick.check.regel.RegelErgebnis;
import de.halloklick.check.regel.RegelPruefer;
import de.halloklick.gemeinsam.ApiFehler;
import de.halloklick.gemeinsam.DatenauskunftBeitrag;
import de.halloklick.gemeinsam.Kategorie;
import de.halloklick.gemeinsam.Sprache;
import de.halloklick.gemeinsam.Stufe;
import de.halloklick.gemeinsam.Texte;
import de.halloklick.ki.KiDienst;
import de.halloklick.ki.KiEinschaetzung;
import de.halloklick.ki.KiErgebnis;
import de.halloklick.ki.PruefEingabe;

/**
 * Der Sicherheitscheck "Ist das echt?" in drei Stufen:
 * <ol>
 *   <li>Regel-Prüfung in Java (kostenlos)</li>
 *   <li>KI, aber nur bei Fotos und unklaren Texten</li>
 *   <li>Zusammenführen: Die höchste Warnstufe gewinnt</li>
 * </ol>
 * Danach wird der Inhalt verworfen. Gespeichert werden nur Stufe und Kategorie.
 */
@Service
public class CheckService implements DatenauskunftBeitrag {

    static final int HOECHSTE_TEXTLAENGE = 5000;

    private final RegelPruefer regeln;
    private final KiDienst ki;
    private final BildPruefung bildPruefung;
    private final SicherheitscheckRepository checks;
    private final FeedbackRepository feedbacks;
    private final Clock uhr;
    private final Texte texte;

    CheckService(RegelPruefer regeln, KiDienst ki, BildPruefung bildPruefung, SicherheitscheckRepository checks,
            FeedbackRepository feedbacks, Clock uhr, Texte texte) {
        this.texte = texte;
        this.regeln = regeln;
        this.ki = ki;
        this.bildPruefung = bildPruefung;
        this.checks = checks;
        this.feedbacks = feedbacks;
        this.uhr = uhr;
    }

    // Absichtlich ohne @Transactional: Während des KI-Aufrufs soll keine Datenbank-Verbindung blockiert sein.
    public CheckErgebnis pruefeText(UUID kontoId, String eingabe) {
        String text = eingabe == null ? "" : eingabe.strip();
        if (text.isEmpty()) {
            throw ApiFehler.ungueltig("Bitte fügen Sie zuerst den Text der Nachricht ein.");
        }
        if (text.length() > HOECHSTE_TEXTLAENGE) {
            throw ApiFehler.ungueltig("Der Text ist zu lang. Bitte fügen Sie nur die eine Nachricht ein.");
        }
        RegelErgebnis regel = regeln.pruefe(text);
        // Kostenbremse, Ebene 1: Die KI wird nur gefragt, wenn die Regeln nicht klar einordnen.
        KiErgebnis kiErgebnis = regel.unklar() ? ki.pruefe(kontoId, PruefEingabe.ausText(text, antwortSprache())) : null;
        return fuehreZusammen(kontoId, Quelle.TEXT, regel, kiErgebnis);
    }

    public CheckErgebnis pruefeBild(UUID kontoId, byte[] datei) {
        byte[] jpeg = bildPruefung.pruefeUndKodiereNeu(datei);
        // Ein Foto muss erst gelesen werden. Das kann nur die KI.
        KiErgebnis kiErgebnis = ki.pruefe(kontoId, PruefEingabe.ausBild(jpeg, antwortSprache()));
        if (kiErgebnis.einschaetzung() == null) {
            return nichtGeprueft(kontoId, kiErgebnis);
        }
        // Auf dem gelesenen Text laufen dieselben Regeln wie bei einer eingefügten Nachricht.
        RegelErgebnis regel = regeln.pruefe(kiErgebnis.einschaetzung().erkannterText());
        return fuehreZusammen(kontoId, Quelle.BILD, regel, kiErgebnis);
    }

    /**
     * Stufe 3: Regeln und KI zusammenführen.
     *
     * <p>Schutz vor Manipulation: Die KI kann die Stufe nur ERHÖHEN, nie unter das
     * Regel-Ergebnis senken. Ihr Text wird nur gezeigt, wenn er zur endgültigen Stufe passt.
     * Sagt eine Betrugsnachricht "Ich bin sicher" und die KI fällt darauf herein,
     * bleibt es trotzdem beim Rot der Regeln und bei unseren festen Texten.
     */
    private CheckErgebnis fuehreZusammen(UUID kontoId, Quelle quelle, RegelErgebnis regel, KiErgebnis kiErgebnis) {
        Stufe stufe = regel.stufe();
        Kategorie kategorie = regel.kategorie();
        List<String> merkmale = new ArrayList<>(regel.merkmale().stream().map(Merkmal::erklaerung).toList());
        String erklaerung = null;
        String naechsterSchritt = null;
        String zusatzHinweis = null;
        boolean kiVerwendet = false;

        if (kiErgebnis != null) {
            KiEinschaetzung einschaetzung = kiErgebnis.einschaetzung();
            if (einschaetzung == null) {
                // KI abgelehnt, ausgefallen oder Limit erreicht: Ergebnis nur aus den Regeln, plus Hinweis
                zusatzHinweis = Textbausteine.kiNichtVerfuegbar(kiErgebnis.grund());
            } else {
                kiVerwendet = true;
                stufe = Stufe.hoechste(stufe, einschaetzung.stufe());
                if (kategorie == Kategorie.SONSTIGES) {
                    kategorie = einschaetzung.kategorie();
                }
                if (einschaetzung.stufe() == stufe) {
                    erklaerung = Textbausteine.entschaerfe(einschaetzung.erklaerung(), 400);
                    naechsterSchritt = Textbausteine.naechsterSchritt(einschaetzung.naechsterSchritt());
                    einschaetzung.merkmale().stream()
                            .map(m -> Textbausteine.entschaerfe(m, 140))
                            .filter(m -> m != null && !merkmale.contains(m))
                            .limit(4)
                            .forEach(merkmale::add);
                }
            }
        }
        if (erklaerung == null) {
            erklaerung = Textbausteine.erklaerung(stufe, !merkmale.isEmpty());
        }
        if (naechsterSchritt == null || stufe == Stufe.ROT && kategorie != Kategorie.SONSTIGES) {
            // Bei Rot ist der feste Text zur Kategorie genauer als die allgemeine Auswahl der KI
            naechsterSchritt = Textbausteine.naechsterSchritt(stufe, kategorie);
        }

        Sicherheitscheck gespeichert = speichere(kontoId, quelle, stufe, kategorie, kiVerwendet);
        // Feste Texte werden in die Sprache der Anfrage übersetzt. Texte der KI sind schon in dieser Sprache.
        return new CheckErgebnis(gespeichert.getId(), stufe, kategorie, true, texte.hier(Textbausteine.titel(stufe)),
                texte.hier(erklaerung), merkmale.stream().map(texte::hier).toList(), texte.hier(naechsterSchritt),
                texte.hier(Textbausteine.HINWEIS), texte.hier(zusatzHinweis),
                stufe == Stufe.ORANGE ? texte.hier(Textbausteine.TIPP_BEI_ORANGE) : null, kiVerwendet);
    }

    /** Foto konnte nicht gelesen werden: ehrlich sagen, dass nichts geprüft wurde. */
    private CheckErgebnis nichtGeprueft(UUID kontoId, KiErgebnis kiErgebnis) {
        Sicherheitscheck gespeichert = speichere(kontoId, Quelle.BILD, Stufe.ORANGE, Kategorie.SONSTIGES, false);
        return new CheckErgebnis(gespeichert.getId(), Stufe.ORANGE, Kategorie.SONSTIGES, false,
                texte.hier(Textbausteine.FOTO_NICHT_GEPRUEFT_TITEL),
                texte.hier(Textbausteine.FOTO_NICHT_GEPRUEFT_ERKLAERUNG), List.of(),
                texte.hier(Textbausteine.naechsterSchritt(Stufe.ORANGE, Kategorie.SONSTIGES)),
                texte.hier(Textbausteine.HINWEIS), texte.hier(Textbausteine.kiNichtVerfuegbar(kiErgebnis.grund())),
                texte.hier(Textbausteine.TIPP_BEI_ORANGE), false);
    }

    /** In dieser Sprache soll die KI ihre Erklärung schreiben. */
    private static String antwortSprache() {
        return Sprache.von(LocaleContextHolder.getLocale()).deutscherName();
    }

    private Sicherheitscheck speichere(UUID kontoId, Quelle quelle, Stufe stufe, Kategorie kategorie,
            boolean kiVerwendet) {
        // Hier kommt bewusst kein Text und kein Bild an: Der Inhalt ist ab jetzt verworfen.
        return checks.save(new Sicherheitscheck(kontoId, quelle, stufe, kategorie, kiVerwendet, uhr.instant()));
    }

    @Transactional
    public void gibFeedback(UUID kontoId, UUID checkId, boolean hilfreich) {
        checks.findById(checkId)
                .filter(check -> check.getKontoId().equals(kontoId))
                .orElseThrow(() -> ApiFehler.nichtGefunden("Diese Prüfung gibt es nicht."));
        Instant jetzt = uhr.instant();
        feedbacks.findById(checkId).ifPresentOrElse(
                vorhanden -> vorhanden.setHilfreich(hilfreich),
                () -> feedbacks.save(new Feedback(checkId, hilfreich, jetzt)));
    }

    // --- Datenauskunft ---

    @Override
    public String name() {
        return "sicherheitschecks";
    }

    @Override
    @Transactional(readOnly = true)
    public Object datenVon(UUID kontoId) {
        return checks.findByKontoIdOrderByErstelltAmDesc(kontoId).stream()
                .map(check -> Map.of(
                        "erstelltAm", check.getErstelltAm(),
                        "quelle", check.getQuelle(),
                        "stufe", check.getStufe(),
                        "kategorie", check.getKategorie(),
                        "kiVerwendet", check.isKiVerwendet()))
                .toList();
    }
}
