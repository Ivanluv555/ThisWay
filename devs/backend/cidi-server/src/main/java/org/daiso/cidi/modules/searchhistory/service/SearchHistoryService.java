/**
 * SearchHistoryService
 *
 * 说明：本文件定义了 SearchHistoryService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.searchhistory.service;

import org.daiso.cidi.modules.searchhistory.entity.SearchHistory;
import org.daiso.cidi.modules.searchhistory.repository.SearchHistoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
