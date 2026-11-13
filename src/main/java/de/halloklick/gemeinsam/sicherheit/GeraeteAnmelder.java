package de.halloklick.gemeinsam.sicherheit;

import java.util.Optional;

/** Prüft einen Geräte-Schlüssel. Die Umsetzung liegt im Modul "konto". */
public interface GeraeteAnmelder {

    Optional<Angemeldet> anmelden(String geraeteSchluessel);
}
