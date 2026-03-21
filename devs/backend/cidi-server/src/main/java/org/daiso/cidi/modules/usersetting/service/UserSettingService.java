package org.daiso.cidi.modules.usersetting.service;

import org.daiso.cidi.modules.usersetting.entity.UserSetting;
import org.daiso.cidi.modules.usersetting.repository.UserSettingRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository repository;

    public List<UserSetting> findAll() {
        return repository.findAll();
    }

    public Optional<UserSetting> findById(Long id) {
        return repository.findById(id);
    }

    public UserSetting save(UserSetting entity) {
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
