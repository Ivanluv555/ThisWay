package org.daiso.cidi.modules.feedbackimage.service;

import org.daiso.cidi.modules.feedbackimage.entity.FeedbackImage;
import org.daiso.cidi.modules.feedbackimage.repository.FeedbackImageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 反馈附件图片元数据的基础应用服务，封装对 {@link FeedbackImageRepository} 的访问。
 *
 * <p>维护反馈与图片地址的关联，文件内容由图片 URL 指向的资源承载。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
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
