/**
 * CategoryService
 *
 * 说明：本文件定义了 CategoryService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.category.service;

import org.daiso.cidi.modules.category.entity.Category;
import org.daiso.cidi.modules.category.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
