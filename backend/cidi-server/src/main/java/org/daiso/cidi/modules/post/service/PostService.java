package org.daiso.cidi.modules.post.service;

import org.daiso.cidi.modules.post.entity.Post;
import org.daiso.cidi.modules.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 社区帖子的基础应用服务，封装对 {@link PostRepository} 的访问。
 *
 * <p>维护帖子正文、所属分类、关联景点及计数字段，图片和评论由各自模块独立维护。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository repository;

    public List<Post> findAll() {
        return repository.findAll();
    }

    public Optional<Post> findById(Long id) {
        return repository.findById(id);
    }

    public Post save(Post entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
