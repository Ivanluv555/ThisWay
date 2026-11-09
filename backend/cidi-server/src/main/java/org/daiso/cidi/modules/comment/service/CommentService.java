package org.daiso.cidi.modules.comment.service;

import org.daiso.cidi.modules.comment.entity.Comment;
import org.daiso.cidi.modules.comment.repository.CommentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 帖子评论的基础应用服务，封装对 {@link CommentRepository} 的访问。
 *
 * <p>维护评论正文及回复关系，按记录查询和保存，不组装评论树。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository repository;

    public List<Comment> findAll() {
        return repository.findAll();
    }

    public Optional<Comment> findById(Long id) {
        return repository.findById(id);
    }

    public Comment save(Comment entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
