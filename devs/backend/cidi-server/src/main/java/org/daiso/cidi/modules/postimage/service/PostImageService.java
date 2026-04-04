/**
 * PostImageService
 *
 * 说明：本文件定义了 PostImageService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.postimage.service;

import org.daiso.cidi.modules.postimage.entity.PostImage;
import org.daiso.cidi.modules.postimage.repository.PostImageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
