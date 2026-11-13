package de.halloklick.ki;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Zähler für die Kostenbremse. Hier wird direkt SQL benutzt, weil "erhöhe den Zähler,
 * aber nur wenn die Grenze nicht erreicht ist" in einem einzigen Befehl passieren muss.
 * Sonst könnten zwei gleichzeitige Anfragen beide durchrutschen.
 */
@Repository
class NutzungRepository {

    record Tagesverbrauch(LocalDate datum, int aufrufe, long eingabeTokens, long ausgabeTokens) {
    }

    record KontoNutzung(LocalDate datum, int kiAufrufe) {
    }

    private final JdbcClient jdbc;

    NutzungRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Reserviert einen KI-Aufruf für heute. Gibt false zurück, wenn das Tageslimit erreicht ist. */
    boolean reserviere(UUID kontoId, LocalDate heute, int tageslimit) {
        int geaendert = jdbc.sql("""
                INSERT INTO nutzung (konto_id, datum, ki_aufrufe)
                SELECT :konto, :datum, 1 WHERE :limit > 0
                ON CONFLICT (konto_id, datum)
                DO UPDATE SET ki_aufrufe = nutzung.ki_aufrufe + 1
                WHERE nutzung.ki_aufrufe < :limit
                """)
                .param("konto", kontoId)
                .param("datum", heute)
                .param("limit", tageslimit)
                .update();
        return geaendert == 1;
    }

    void verbuche(LocalDate heute, long eingabeTokens, long ausgabeTokens) {
        jdbc.sql("""
                INSERT INTO ki_tagesverbrauch (datum, aufrufe, eingabe_tokens, ausgabe_tokens)
                VALUES (:datum, 1, :eingabe, :ausgabe)
                ON CONFLICT (datum)
                DO UPDATE SET aufrufe = ki_tagesverbrauch.aufrufe + 1,
                              eingabe_tokens = ki_tagesverbrauch.eingabe_tokens + :eingabe,
                              ausgabe_tokens = ki_tagesverbrauch.ausgabe_tokens + :ausgabe
                """)
                .param("datum", heute)
                .param("eingabe", eingabeTokens)
                .param("ausgabe", ausgabeTokens)
                .update();
    }

    List<Tagesverbrauch> verbrauchSeit(LocalDate von) {
        return jdbc.sql("""
                SELECT datum, aufrufe, eingabe_tokens, ausgabe_tokens
                FROM ki_tagesverbrauch WHERE datum >= :von ORDER BY datum DESC
                """)
                .param("von", von)
                .query((rs, zeile) -> new Tagesverbrauch(rs.getObject("datum", LocalDate.class),
                        rs.getInt("aufrufe"), rs.getLong("eingabe_tokens"), rs.getLong("ausgabe_tokens")))
                .list();
    }

    List<KontoNutzung> nutzungVon(UUID kontoId) {
        return jdbc.sql("SELECT datum, ki_aufrufe FROM nutzung WHERE konto_id = :konto ORDER BY datum DESC")
                .param("konto", kontoId)
                .query((rs, zeile) -> new KontoNutzung(rs.getObject("datum", LocalDate.class),
                        rs.getInt("ki_aufrufe")))
                .list();
    }
}
