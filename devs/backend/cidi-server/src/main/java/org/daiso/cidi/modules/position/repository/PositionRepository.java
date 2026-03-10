package org.daiso.cidi.modules.position.repository;

import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PositionRepository extends JpaRepository<AbstractAuditable, Integer> {
}
