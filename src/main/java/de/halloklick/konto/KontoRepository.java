package de.halloklick.konto;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface KontoRepository extends JpaRepository<Konto, UUID> {

    Optional<Konto> findByEmail(String email);

    boolean existsByEmail(String email);
}
