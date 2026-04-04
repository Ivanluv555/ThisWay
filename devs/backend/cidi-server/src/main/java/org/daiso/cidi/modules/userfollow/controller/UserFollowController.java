/**
 * UserFollowController
 *
 * 说明：本文件定义了 UserFollowController，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.userfollow.controller;

import org.daiso.cidi.core.error.CidiException;
import org.daiso.cidi.core.error.ErrorCode;

import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.daiso.cidi.modules.userfollow.service.UserFollowService;
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
@RequestMapping("/api/user-follows")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService service;

    @GetMapping
    public List<UserFollow> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserFollow> getById(@PathVariable Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new CidiException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在: id=" + id));
    }

    @PostMapping
    public UserFollow create(@RequestBody UserFollow entity) {
        return service.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserFollow> update(@PathVariable Long id, @RequestBody UserFollow entity) {
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
