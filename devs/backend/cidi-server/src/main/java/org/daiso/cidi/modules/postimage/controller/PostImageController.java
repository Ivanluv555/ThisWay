package org.daiso.cidi.modules.postimage.controller;

import org.daiso.cidi.modules.postimage.entity.PostImage;
import org.daiso.cidi.modules.postimage.service.PostImageService;
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
@RequestMapping("/api/post-images")
@RequiredArgsConstructor
public class PostImageController {

    private final PostImageService service;

    @GetMapping
    public List<PostImage> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostImage> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public PostImage create(@RequestBody PostImage entity) {
        return service.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostImage> update(@PathVariable Long id, @RequestBody PostImage entity) {
        return service.findById(id)
            .map(existing -> {
                entity.setId(id);
                return ResponseEntity.ok(service.save(entity));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
