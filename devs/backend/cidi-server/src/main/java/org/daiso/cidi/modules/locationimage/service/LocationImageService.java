package org.daiso.cidi.modules.locationimage.service;

import org.daiso.cidi.modules.locationimage.entity.LocationImage;
import org.daiso.cidi.modules.locationimage.repository.LocationImageRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
