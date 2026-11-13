package de.digitalbegleiter.konto;

import java.util.UUID;

/**
 * Wird verschickt, kurz bevor ein Konto gelöscht wird. Andere Module können
 * darauf hören und aufräumen, ohne dass "konto" sie kennen muss.
 */
public record KontoWirdGeloeschtEvent(UUID kontoId) {
}
