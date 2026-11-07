/**
 * SearchHistoryRepository
 *
 * 说明：本文件定义了 SearchHistoryRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.searchhistory.repository;

import org.daiso.cidi.modules.searchhistory.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
}
