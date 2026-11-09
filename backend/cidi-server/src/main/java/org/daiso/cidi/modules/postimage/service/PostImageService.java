package org.daiso.cidi.modules.postimage.service;

import org.daiso.cidi.modules.postimage.entity.PostImage;
import org.daiso.cidi.modules.postimage.repository.PostImageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 帖子图片元数据的基础应用服务，封装对 {@link PostImageRepository} 的访问。
 *
 * <p>维护帖子图片的地址、尺寸和顺序，持久化对象为图片元数据。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class PostImageService {

    private final PostImageRepository repository;

    public List<PostImage> findAll() {
        return repository.findAll();
    }

    public Optional<PostImage> findById(Long id) {
        return repository.findById(id);
    }

    public PostImage save(PostImage entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
