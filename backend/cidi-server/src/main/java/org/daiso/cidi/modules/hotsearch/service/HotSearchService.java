package org.daiso.cidi.modules.hotsearch.service;

import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.daiso.cidi.modules.hotsearch.repository.HotSearchRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 热搜关键词的基础应用服务，封装对 {@link HotSearchRepository} 的访问。
 *
 * <p>维护热搜词及其热度、启用标记，热度由写入方提供。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class HotSearchService {

    private final HotSearchRepository repository;

    public List<HotSearch> findAll() {
        return repository.findAll();
    }

    public Optional<HotSearch> findById(Integer id) {
        return repository.findById(id);
    }

    public HotSearch save(HotSearch entity) {
        return repository.save(entity);
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }
}
