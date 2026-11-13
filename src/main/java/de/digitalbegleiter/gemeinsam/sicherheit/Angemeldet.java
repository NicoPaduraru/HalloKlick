package de.digitalbegleiter.gemeinsam.sicherheit;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Wer gerade angemeldet ist. Senioren melden sich über ihr Gerät an (geraetId gesetzt),
 * Angehörige über E-Mail und Passwort (geraetId ist null).
 */
public record Angemeldet(UUID kontoId, String rolle, UUID geraetId) implements Serializable {

    public static final String SENIOR = "SENIOR";
    public static final String ANGEHOERIGER = "ANGEHOERIGER";

    public boolean istSenior() {
        return SENIOR.equals(rolle);
    }

    public Authentication alsAuthentication() {
        return UsernamePasswordAuthenticationToken.authenticated(
                this, null, List.of(new SimpleGrantedAuthority("ROLE_" + rolle)));
    }
}
