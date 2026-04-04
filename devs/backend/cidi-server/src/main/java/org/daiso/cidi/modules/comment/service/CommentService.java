/**
 * CommentService
 *
 * 说明：本文件定义了 CommentService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.comment.service;

import org.daiso.cidi.modules.comment.entity.Comment;
import org.daiso.cidi.modules.comment.repository.CommentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
