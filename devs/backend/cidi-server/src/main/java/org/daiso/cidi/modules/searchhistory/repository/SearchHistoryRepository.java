package org.daiso.cidi.modules.searchhistory.repository;

import org.daiso.cidi.modules.searchhistory.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
}
