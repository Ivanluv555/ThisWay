package org.daiso.cidi.modules.locationimage.service;

import org.daiso.cidi.modules.locationimage.entity.LocationImage;
import org.daiso.cidi.modules.locationimage.repository.LocationImageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 景点图片元数据的基础应用服务，封装对 {@link LocationImageRepository} 的访问。
 *
 * <p>维护景点与图片地址的关联及展示属性，文件内容由图片 URL 指向的资源承载。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class LocationImageService {

    private final LocationImageRepository repository;

    public List<LocationImage> findAll() {
        return repository.findAll();
    }

    public Optional<LocationImage> findById(Long id) {
        return repository.findById(id);
    }

    public LocationImage save(LocationImage entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
