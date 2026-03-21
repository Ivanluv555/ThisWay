package org.daiso.cidi.modules.hotsearch.controller;

import org.daiso.cidi.modules.hotsearch.entity.HotSearch;
import org.daiso.cidi.modules.hotsearch.service.HotSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hot-searches")
@RequiredArgsConstructor
public class HotSearchController {

    private final HotSearchService service;

    @GetMapping
    public List<HotSearch> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotSearch> getById(@PathVariable Integer id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public HotSearch create(@RequestBody HotSearch entity) {
        return service.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotSearch> update(@PathVariable Integer id, @RequestBody HotSearch entity) {
        return service.findById(id)
            .map(existing -> {
                entity.setId(id);
                return ResponseEntity.ok(service.save(entity));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
