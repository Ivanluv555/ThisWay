package org.daiso.cidi.modules.postimage.controller;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;

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

/**
 * 帖子图片元数据的 REST 入口，基础路径为 {@code /api/post-images}。
 *
 * <p>委托 {@link PostImageService} 提供列表、详情、新建、更新和删除操作，请求体与响应直接使用实体。
 *
 * <p>更新先确认记录存在，再以路径 ID 保存请求实体；查询、更新或删除目标不存在时抛出资源不存在异常，删除成功返回 HTTP 204。
 */
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
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
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
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.findById(id).isEmpty()) {
            throw new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id);
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
