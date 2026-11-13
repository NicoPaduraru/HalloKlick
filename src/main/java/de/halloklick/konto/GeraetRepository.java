package de.halloklick.konto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface GeraetRepository extends JpaRepository<Geraet, UUID> {

    Optional<Geraet> findByTokenHash(String tokenHash);

    List<Geraet> findByKontoIdOrderByErstelltAm(UUID kontoId);

    List<Geraet> findByZuletztGesehenBefore(Instant grenze);

    boolean existsByKontoIdAndZuletztGesehenGreaterThanEqual(UUID kontoId, Instant grenze);
}
