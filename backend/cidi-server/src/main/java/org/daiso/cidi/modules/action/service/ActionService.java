package org.daiso.cidi.modules.action.service;

import org.daiso.cidi.modules.action.entity.Action;
import org.daiso.cidi.modules.action.repository.ActionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 帖子互动记录的基础应用服务，封装对 {@link ActionRepository} 的访问。
 *
 * <p>保留操作类型和时间供调用方解释，当前接口按记录主键维护数据。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
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
