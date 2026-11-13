package de.digitalbegleiter.benachrichtigung;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PushAboRepository extends JpaRepository<PushAbo, UUID> {

    List<PushAbo> findByKontoId(UUID kontoId);

    Optional<PushAbo> findByEndpoint(String endpoint);

    long countByKontoId(UUID kontoId);
}
