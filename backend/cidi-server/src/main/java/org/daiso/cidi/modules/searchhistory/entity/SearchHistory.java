package org.daiso.cidi.modules.searchhistory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户搜索历史的 JPA 持久化实体，映射到 {@code search_history} 表。
 *
 * <p>通过 userId 标识搜索用户，记录关键词、创建时间和删除标记 isDeleted。
 *
 * <p>isDeleted 仅作为存储字段；当前列表未过滤该标记，删除接口执行物理删除。
 */
@Entity
@Table(name = "search_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String keyword;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

}
