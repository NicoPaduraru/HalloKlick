package de.halloklick.gemeinsam.sicherheit;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Meldet ein Senioren-Gerät über sein Cookie an. Läuft bei jeder Anfrage,
 * es wird keine Sitzung angelegt. Eine Anmeldung als Angehöriger hat Vorrang.
 */
public class GeraeteCookieFilter extends OncePerRequestFilter {

    private final GeraeteAnmelder anmelder;

    public GeraeteCookieFilter(GeraeteAnmelder anmelder) {
        this.anmelder = anmelder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest anfrage, HttpServletResponse antwort, FilterChain kette)
            throws ServletException, IOException {
        Authentication bisher = SecurityContextHolder.getContext().getAuthentication();
        boolean nichtAngemeldet = bisher == null || bisher instanceof AnonymousAuthenticationToken
                || !bisher.isAuthenticated();
        if (nichtAngemeldet) {
            GeraeteCookie.lies(anfrage).flatMap(anmelder::anmelden).ifPresent(angemeldet -> {
                SecurityContext kontext = SecurityContextHolder.createEmptyContext();
                kontext.setAuthentication(angemeldet.alsAuthentication());
                SecurityContextHolder.setContext(kontext);
            });
        }
        kette.doFilter(anfrage, antwort);
    }
}
