/**
 * ActionService
 *
 * 说明：本文件定义了 ActionService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.action.service;

import org.daiso.cidi.modules.action.entity.Action;
import org.daiso.cidi.modules.action.repository.ActionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionRepository repository;

    public List<Action> findAll() {
        return repository.findAll();
    }

    public Optional<Action> findById(Long id) {
        return repository.findById(id);
    }

    public Action save(Action entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
