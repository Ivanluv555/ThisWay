package org.daiso.cidi.modules.hotsearch.service;

import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.daiso.cidi.modules.hotsearch.repository.HotSearchRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
