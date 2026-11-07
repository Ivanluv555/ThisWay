/**
 * HotSearchRepository
 *
 * 说明：本文件定义了 HotSearchRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.hotsearch.repository;

import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HotSearchRepository extends JpaRepository<HotSearch, Integer> {
}
