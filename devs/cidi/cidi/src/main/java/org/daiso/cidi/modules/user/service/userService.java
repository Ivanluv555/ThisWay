package org.daiso.cidi.modules.user.service;

import org.daiso.cidi.modules.user.entity.user;
import org.daiso.cidi.modules.user.repository.userRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class userService {
    private final userRepository userRepository; //注入控制器

    @Autowired
    public userService(userRepository userRepository) {
        this.userRepository = userRepository;
    }

    //这里写业务逻辑
    //首先是注册
    public user register_user(String userID, String password, String username) throws Exception{

    }

}