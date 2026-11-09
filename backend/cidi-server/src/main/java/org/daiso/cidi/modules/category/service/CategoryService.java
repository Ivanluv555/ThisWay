package org.daiso.cidi.modules.category.service;

import org.daiso.cidi.modules.category.entity.Category;
import org.daiso.cidi.modules.category.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 帖子分类的基础应用服务，封装对 {@link CategoryRepository} 的访问。
 *
 * <p>维护帖子分类及展示配置，列表结果沿用持久层的默认查询行为。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;

    public List<Category> findAll() {
        return repository.findAll();
    }

    public Optional<Category> findById(Integer id) {
        return repository.findById(id);
    }

    public Category save(Category entity) {
        return repository.save(entity);
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }
}
