/**
 * UserService
 *
 * 说明：本文件定义了 UserService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.user.service;

import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
