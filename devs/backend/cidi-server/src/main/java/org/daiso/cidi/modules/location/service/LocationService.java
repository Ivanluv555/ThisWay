/**
 * LocationService
 *
 * 说明：本文件定义了 LocationService，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.location.service;

import org.daiso.cidi.modules.location.entity.Location;
import org.daiso.cidi.modules.location.repository.LocationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
