package org.daiso.cidi.modules.searchhistory.service;

import org.daiso.cidi.modules.searchhistory.entity.SearchHistory;
import org.daiso.cidi.modules.searchhistory.repository.SearchHistoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户搜索历史的基础应用服务，封装对 {@link SearchHistoryRepository} 的访问。
 *
 * <p>维护用户搜索记录；基础查询返回全部记录，deleteById 直接删除数据库行。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository repository;

    public List<SearchHistory> findAll() {
        return repository.findAll();
    }

    public Optional<SearchHistory> findById(Long id) {
        return repository.findById(id);
    }

    public SearchHistory save(SearchHistory entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
