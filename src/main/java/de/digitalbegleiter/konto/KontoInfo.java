package de.digitalbegleiter.konto;

import java.time.Instant;
import java.util.UUID;

/** Was andere Module und die Web-App über ein Konto wissen dürfen (DTO, nie die Entity selbst). */
public record KontoInfo(UUID id, KontoTyp typ, String anzeigename, String email, Tarif tarif, Instant erstelltAm) {

    static KontoInfo von(Konto konto) {
        return new KontoInfo(konto.getId(), konto.getTyp(), konto.getAnzeigename(), konto.getEmail(),
                konto.getTarif(), konto.getErstelltAm());
    }
}
