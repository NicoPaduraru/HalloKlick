package de.digitalbegleiter.gemeinsam;

import org.springframework.http.HttpStatus;

/**
 * Ein Fehler, den der Nutzer sehen darf. Der Text ist in einfacher Sprache
 * und wird als ProblemDetail (RFC 9457) zurückgegeben.
 */
public class ApiFehler extends RuntimeException {

    private final HttpStatus status;

    public ApiFehler(HttpStatus status, String nachricht) {
        super(nachricht);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static ApiFehler nichtGefunden(String nachricht) {
        return new ApiFehler(HttpStatus.NOT_FOUND, nachricht);
    }

    public static ApiFehler ungueltig(String nachricht) {
        return new ApiFehler(HttpStatus.BAD_REQUEST, nachricht);
    }

    public static ApiFehler konflikt(String nachricht) {
        return new ApiFehler(HttpStatus.CONFLICT, nachricht);
    }

    public static ApiFehler keinZugriff() {
        return new ApiFehler(HttpStatus.FORBIDDEN, "Das ist für Sie nicht erlaubt.");
    }

    public static ApiFehler abgeschaltet() {
        return new ApiFehler(HttpStatus.SERVICE_UNAVAILABLE, "Diese Funktion ist gerade abgeschaltet.");
    }
}
