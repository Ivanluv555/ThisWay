package org.daiso.cidi.modules.admin.service;

import org.daiso.cidi.modules.admin.entity.Admin;
import org.daiso.cidi.modules.admin.repository.AdminRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理员资料的基础应用服务，封装对 {@link AdminRepository} 的访问。
 *
 * <p>维护管理员记录及角色资料，保存过程直接委托持久层。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository repository;

    public List<Admin> findAll() {
        return repository.findAll();
    }

    public Optional<Admin> findById(Integer id) {
        return repository.findById(id);
    }

    public Admin save(Admin entity) {
        return repository.save(entity);
    }

    public void deleteById(Integer id) {
        repository.deleteById(id);
    }
}
