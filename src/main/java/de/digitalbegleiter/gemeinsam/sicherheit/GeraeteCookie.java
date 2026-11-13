package de.digitalbegleiter.gemeinsam.sicherheit;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import de.digitalbegleiter.gemeinsam.Einstellungen;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Der Geräte-Schlüssel liegt in einem Cookie, das JavaScript nicht lesen kann (HttpOnly).
 * Das ist sicherer als ein Schlüssel im Browser-Speicher.
 */
@Component
public class GeraeteCookie {

    public static final String NAME = "DBGERAET";

    private final boolean secure;

    public GeraeteCookie(Einstellungen einstellungen) {
        this.secure = einstellungen.cookies().secure();
    }

    public void setze(HttpServletResponse antwort, String schluessel) {
        antwort.addHeader(HttpHeaders.SET_COOKIE, baue(schluessel, Duration.ofDays(730)).toString());
    }

    public void entferne(HttpServletResponse antwort) {
        antwort.addHeader(HttpHeaders.SET_COOKIE, baue("", Duration.ZERO).toString());
    }

    public static Optional<String> lies(HttpServletRequest anfrage) {
        if (anfrage.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : anfrage.getCookies()) {
            if (NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie baue(String wert, Duration dauer) {
        return ResponseCookie.from(NAME, wert)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(dauer)
                .build();
    }
}
