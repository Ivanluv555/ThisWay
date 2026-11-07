/**
 * FeedbackService
 *
 * 说明：本文件定义了 FeedbackService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.feedback.service;

import org.daiso.cidi.modules.feedback.entity.Feedback;
import org.daiso.cidi.modules.feedback.repository.FeedbackRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository repository;

    public List<Feedback> findAll() {
        return repository.findAll();
    }

    public Optional<Feedback> findById(Long id) {
        return repository.findById(id);
    }

    public Feedback save(Feedback entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
