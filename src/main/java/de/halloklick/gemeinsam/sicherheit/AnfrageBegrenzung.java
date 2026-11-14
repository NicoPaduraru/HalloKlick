package de.halloklick.gemeinsam.sicherheit;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.halloklick.gemeinsam.Einstellungen;
import de.halloklick.gemeinsam.Sprache;
import de.halloklick.gemeinsam.Texte;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Begrenzt die Anfragen pro Minute und Internet-Adresse (Rate Limiting).
 * Heikle Wege (Anmelden, Koppeln, Prüfen) haben eine strengere Grenze,
 * damit niemand Passwörter oder Einladungscodes durchprobieren kann.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AnfrageBegrenzung extends OncePerRequestFilter {

    private record Zaehler(long minute, int anzahl) {
    }

    private final Map<String, Zaehler> zaehler = new ConcurrentHashMap<>();
    private final int proMinute;
    private final int heikelProMinute;
    private final Clock uhr;
    private final ObjectMapper json;
    private final Texte texte;

    public AnfrageBegrenzung(Einstellungen einstellungen, Clock uhr, ObjectMapper json, Texte texte) {
        this.texte = texte;
        this.proMinute = einstellungen.begrenzung().proMinute();
        this.heikelProMinute = einstellungen.begrenzung().heikelProMinute();
        this.uhr = uhr;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest anfrage) {
        return !anfrage.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest anfrage, HttpServletResponse antwort, FilterChain kette)
            throws ServletException, IOException {
        String adresse = anfrage.getRemoteAddr();
        boolean heikel = istHeikel(anfrage);
        boolean erlaubt = erlaube("alle|" + adresse, proMinute)
                && (!heikel || erlaube("heikel|" + adresse, heikelProMinute));
        if (!erlaubt) {
            antwort.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            antwort.setHeader("Retry-After", "60");
            antwort.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            antwort.setCharacterEncoding("UTF-8");
            json.writeValue(antwort.getWriter(), ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                    texte.in(Sprache.von(anfrage), "Das waren zu viele Versuche. Bitte warten Sie eine Minute.")));
            return;
        }
        kette.doFilter(anfrage, antwort);
    }

    /** Zählt einen Versuch und sagt, ob er noch innerhalb der Grenze liegt. */
    boolean erlaube(String schluessel, int grenze) {
        long minute = uhr.millis() / 60_000;
        Zaehler neu = zaehler.merge(schluessel, new Zaehler(minute, 1),
                (alt, eins) -> alt.minute() == minute ? new Zaehler(minute, alt.anzahl() + 1) : eins);
        return neu.anzahl() <= grenze;
    }

    private static boolean istHeikel(HttpServletRequest anfrage) {
        if (!"POST".equals(anfrage.getMethod())) {
            return false;
        }
        String pfad = anfrage.getRequestURI();
        return pfad.startsWith("/api/auth/") || pfad.startsWith("/api/geraete") || pfad.equals("/api/checks");
    }

    /** Alte Zähler wegräumen, damit der Speicher nicht voll läuft. */
    @Scheduled(fixedRate = 120_000)
    void raeumeAuf() {
        long minute = uhr.millis() / 60_000;
        zaehler.values().removeIf(z -> z.minute() < minute);
    }
}
