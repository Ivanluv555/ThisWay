package org.daiso.cidi.modules.adminuser.repository;

import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminuserRepository extends JpaRepository<AbstractAuditable, Integer> {
}
