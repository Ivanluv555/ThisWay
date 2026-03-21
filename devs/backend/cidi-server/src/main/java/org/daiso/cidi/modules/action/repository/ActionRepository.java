package org.daiso.cidi.modules.action.repository;

import org.daiso.cidi.modules.action.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {
}
