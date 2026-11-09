package org.daiso.cidi.modules.comment.repository;

import org.daiso.cidi.modules.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 帖子评论的 Spring Data JPA 数据访问接口。
 *
 * <p>继承 {@link JpaRepository} 提供的主键查询与增删改操作；主键类型为 {@code Long}。
 *
 * <p>当前未声明业务条件查询，关联 ID、状态和展示顺序由调用方按实际需求处理。
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {
}
