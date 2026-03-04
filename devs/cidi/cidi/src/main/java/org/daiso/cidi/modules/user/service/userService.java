package org.daiso.cidi.modules.user.service;

import org.daiso.cidi.modules.user.dto.UserCreateDto;
import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import org.daiso.cidi.core.utils.WeChatApiUtil;
import org.daiso.cidi.core.utils.JwtTokenUtil;
import org.daiso.cidi.core.exception.WechatLoginException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final WeChatApiUtil weChatApiUtil;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public UserService(UserRepository userRepository,
                       WeChatApiUtil weChatApiUtil,
                       JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.weChatApiUtil = weChatApiUtil;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * Login by WeChat code: obtain openid/unionid and create/retrieve user record
     */
    public String loginWithWechat(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }

        String openid;
        try {
            openid = weChatApiUtil.getUnionIdFromCode(code);
        } catch (Exception e) {
            throw new WechatLoginException("微信登录失败", e);
        }
        if (openid == null || openid.isEmpty()) {
            throw new WechatLoginException("微信未返回 openid/unionid");
        }

        Optional<User> ou = userRepository.findByOpenid(openid);
        User u;
        if (ou.isEmpty()) {
            // create new user with minimal fields
            u = new User();
            u.setOpenid(openid);
            u.setUsername("微信用户");
            u.setCreateTime(LocalDateTime.now());
            u = userRepository.save(u);
        } else {
            u = ou.get();
        }

        // generate token with user id as subject
        String token = jwtTokenUtil.generateToken(u.getId().toString());
        return token;
    }

    // CRUD methods used by controller
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Page<User> list(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User create(UserCreateDto dto) {
        User u = new User();
        u.setOpenid(dto.getOpenid());
        u.setUsername(dto.getUsername());
        u.setAvatarUrl(dto.getAvatarUrl());
        u.setBio(dto.getBio());
        return userRepository.save(u);
    }

    public User update(Long id, UserCreateDto dto) {
        User u = findById(id);
        if (dto.getUsername() != null) u.setUsername(dto.getUsername());
        if (dto.getAvatarUrl() != null) u.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getBio() != null) u.setBio(dto.getBio());
        return userRepository.save(u);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public void deleteAll(List<Long> ids) {
        List<User> list = userRepository.findAllById(ids);
        userRepository.deleteAll(list);
    }
}