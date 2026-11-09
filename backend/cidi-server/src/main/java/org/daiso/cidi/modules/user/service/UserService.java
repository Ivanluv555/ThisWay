package org.daiso.cidi.modules.user.service;

import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 小程序用户资料的基础应用服务，封装对 {@link UserRepository} 的访问。
 *
 * <p>提供用户资料的基础查询、保存和删除；微信登录及手机号绑定由 WechatAuthService 编排。
 *
 * <p>按主键查询通过 {@link Optional} 表达记录缺失，HTTP 层负责转换为资源不存在响应。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public List<User> findAll() {
        return repository.findAll();
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    public User save(User entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
