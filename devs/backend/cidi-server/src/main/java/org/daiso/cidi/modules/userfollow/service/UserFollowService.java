/**
 * UserFollowService
 *
 * 说明：本文件定义了 UserFollowService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.userfollow.service;

import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.daiso.cidi.modules.userfollow.repository.UserFollowRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFollowService {

    private final UserFollowRepository repository;

    public List<UserFollow> findAll() {
        return repository.findAll();
    }

    public Optional<UserFollow> findById(Long id) {
        return repository.findById(id);
    }

    public UserFollow save(UserFollow entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
