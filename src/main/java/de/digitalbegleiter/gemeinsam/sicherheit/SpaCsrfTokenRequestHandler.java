package de.digitalbegleiter.gemeinsam.sicherheit;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CSRF-Schutz für eine Web-App mit JavaScript (Muster aus der Spring-Security-Dokumentation):
 * Der Server legt das Token in das Cookie XSRF-TOKEN, das JavaScript schickt es
 * bei jeder ändernden Anfrage im Header X-XSRF-TOKEN zurück.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler einfach = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler verschleiert = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest anfrage, HttpServletResponse antwort, Supplier<CsrfToken> token) {
        verschleiert.handle(anfrage, antwort, token);
        // Token sofort laden, damit das Cookie bei jeder Antwort gesetzt wird.
        token.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest anfrage, CsrfToken token) {
        String header = anfrage.getHeader(token.getHeaderName());
        return (StringUtils.hasText(header) ? einfach : verschleiert).resolveCsrfTokenValue(anfrage, token);
    }
}
