package de.halloklick.anleitung;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AnleitungRepository extends JpaRepository<Anleitung, UUID> {

    /**
     * Volltextsuche in der gewählten Sprache. Die Suchwörter werden mit ODER verknüpft, damit auch
     * ganze Fragen wie "wie schicke ich ein foto" etwas finden. Die beste Übereinstimmung
     * steht oben. Zusätzlich wird der Titel nach dem getippten Text durchsucht.
     *
     * <p>Allgemeine Anleitungen kommen nur in der gewählten Sprache. Eigene Anleitungen
     * der Familie kommen immer, egal in welcher Sprache sie geschrieben sind.
     *
     * @param suchKonfiguration Sprach-Einstellung der PostgreSQL-Volltextsuche, zum Beispiel "german"
     */
    @Query(nativeQuery = true, value = """
            SELECT a.id
            FROM anleitung a
            CROSS JOIN (SELECT CAST(replace(CAST(plainto_tsquery(CAST(:suchKonfiguration AS regconfig), :suche)
                                                 AS text), '&', '|')
                                    AS tsquery) AS frage) s
            WHERE a.plattform IN (:plattformen)
              AND ((a.familie_id IS NULL AND a.sprache = :sprache) OR a.familie_id IN (:familien))
              AND (a.suchtext @@ s.frage OR a.titel ILIKE :muster ESCAPE '\\')
            ORDER BY ts_rank(a.suchtext, s.frage) DESC, a.titel
            LIMIT 30
            """)
    List<UUID> suche(@Param("suche") String suche, @Param("muster") String muster,
            @Param("plattformen") Collection<String> plattformen, @Param("familien") Collection<UUID> familien,
            @Param("sprache") String sprache, @Param("suchKonfiguration") String suchKonfiguration);

    @Query(nativeQuery = true, value = """
            SELECT a.id
            FROM anleitung a
            WHERE a.plattform IN (:plattformen)
              AND ((a.familie_id IS NULL AND a.sprache = :sprache) OR a.familie_id IN (:familien))
            ORDER BY a.quelle, a.kategorie, a.titel
            LIMIT 200
            """)
    List<UUID> alle(@Param("plattformen") Collection<String> plattformen,
            @Param("familien") Collection<UUID> familien, @Param("sprache") String sprache);

    List<Anleitung> findByFamilieIdOrderByTitel(UUID familieId);

    long countByFamilieId(UUID familieId);
}
