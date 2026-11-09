package org.daiso.cidi.modules.feedback.service;

import org.daiso.cidi.modules.feedback.entity.Feedback;
import org.daiso.cidi.modules.feedback.repository.FeedbackRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户反馈的基础应用服务，封装对 {@link FeedbackRepository} 的访问。
 *
 * <p>维护反馈正文、联系方式和状态，附件地址由反馈图片模块单独保存。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
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
