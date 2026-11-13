package de.halloklick.familie;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FamilieRepository extends JpaRepository<Familie, UUID> {
}

interface MitgliedschaftRepository extends JpaRepository<Mitgliedschaft, MitgliedschaftId> {

    List<Mitgliedschaft> findByIdKontoId(UUID kontoId);

    List<Mitgliedschaft> findByIdFamilieIdOrderByBeigetretenAm(UUID familieId);

    long countByIdKontoIdAndRolle(UUID kontoId, Rolle rolle);

    long countByIdFamilieId(UUID familieId);
}

interface EinladungRepository extends JpaRepository<Einladung, UUID> {

    Optional<Einladung> findByCodeHash(String codeHash);

    /**
     * Markiert die Einladung als verwendet, aber nur wenn sie noch gültig und unbenutzt ist.
     * Das passiert in einem einzigen Befehl, damit ein Code wirklich nur einmal gilt.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Einladung e SET e.verwendet = true "
            + "WHERE e.id = :id AND e.verwendet = false AND e.gueltigBis > :jetzt")
    int loeseEin(@Param("id") UUID id, @Param("jetzt") Instant jetzt);

    @Modifying
    @Query("DELETE FROM Einladung e WHERE e.gueltigBis < :grenze")
    int loescheAbgelaufene(@Param("grenze") Instant grenze);
}

interface HilfeanfrageRepository extends JpaRepository<Hilfeanfrage, UUID> {

    List<Hilfeanfrage> findByFamilieIdAndStatusOrderByErstelltAmDesc(UUID familieId, HilfeStatus status);

    List<Hilfeanfrage> findByFamilieIdOrderByErstelltAmDesc(UUID familieId);

    List<Hilfeanfrage> findByVonKontoIdOrderByErstelltAmDesc(UUID kontoId);

    long countByVonKontoIdAndErstelltAmAfter(UUID kontoId, Instant seit);

    @Modifying
    @Query("DELETE FROM Hilfeanfrage h WHERE h.erstelltAm < :grenze")
    int loescheAelterAls(@Param("grenze") Instant grenze);
}
