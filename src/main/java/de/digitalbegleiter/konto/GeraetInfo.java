package de.digitalbegleiter.konto;

import java.time.Instant;
import java.util.UUID;

public record GeraetInfo(UUID id, UUID kontoId, Plattform plattform, Instant zuletztGesehen, boolean widerrufen) {

    static GeraetInfo von(Geraet geraet) {
        return new GeraetInfo(geraet.getId(), geraet.getKontoId(), geraet.getPlattform(),
                geraet.getZuletztGesehen(), geraet.isWiderrufen());
    }
}
