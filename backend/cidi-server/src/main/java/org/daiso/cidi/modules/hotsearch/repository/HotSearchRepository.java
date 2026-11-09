package org.daiso.cidi.modules.hotsearch.repository;

import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 热搜关键词的 Spring Data JPA 数据访问接口。
 *
 * <p>继承 {@link JpaRepository} 提供的主键查询与增删改操作；主键类型为 {@code Integer}。
 *
 * <p>当前未声明业务条件查询，关联 ID、状态和展示顺序由调用方按实际需求处理。
 */
public interface HotSearchRepository extends JpaRepository<HotSearch, Integer> {
}
