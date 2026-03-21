package org.daiso.cidi.modules.hotsearch.repository;

import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotSearchRepository extends JpaRepository<HotSearch, Integer> {
}
