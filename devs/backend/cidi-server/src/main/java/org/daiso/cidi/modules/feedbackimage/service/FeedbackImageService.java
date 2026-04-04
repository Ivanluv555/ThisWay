/**
 * FeedbackImageService
 *
 * 说明：本文件定义了 FeedbackImageService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.feedbackimage.service;

import org.daiso.cidi.modules.feedbackimage.entity.FeedbackImage;
import org.daiso.cidi.modules.feedbackimage.repository.FeedbackImageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackImageService {

    private final FeedbackImageRepository repository;

    public List<FeedbackImage> findAll() {
        return repository.findAll();
    }

    public Optional<FeedbackImage> findById(Long id) {
        return repository.findById(id);
    }

    public FeedbackImage save(FeedbackImage entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
