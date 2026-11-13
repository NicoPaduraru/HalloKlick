package de.halloklick.check;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SicherheitscheckRepository extends JpaRepository<Sicherheitscheck, UUID> {

    List<Sicherheitscheck> findByKontoIdOrderByErstelltAmDesc(UUID kontoId);
}

interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
