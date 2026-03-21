package org.daiso.cidi.modules.admin.service;

import org.daiso.cidi.modules.admin.entity.Admin;
import org.daiso.cidi.modules.admin.repository.AdminRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
