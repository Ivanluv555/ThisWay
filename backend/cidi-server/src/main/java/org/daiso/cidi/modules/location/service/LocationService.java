package org.daiso.cidi.modules.location.service;

import org.daiso.cidi.modules.location.entity.Location;
import org.daiso.cidi.modules.location.repository.LocationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 景点基础信息的基础应用服务，封装对 {@link LocationRepository} 的访问。
 *
 * <p>维护景点资料；景点名称还由下一目的地服务读取，用于补全转移预测的展示结果。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository repository;

    public List<Location> findAll() {
        return repository.findAll();
    }

    public Optional<Location> findById(Long id) {
        return repository.findById(id);
    }

    public Location save(Location entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
