package org.daiso.cidi.modules.user.service;

import org.daiso.cidi.modules.user.entity.user;
import org.daiso.cidi.modules.user.repository.userRepository;
// 假设您有这些工具类，用于调用微信API和生成JWT
import org.daiso.cidi.core.utils.WeChatApiUtil;
import org.daiso.cidi.core.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
class userService {
    private final userRepository userRepository; //注入控制器

    @Autowired
    private final WeChatApiUtil weChatApiUtil;
    private final JwtTokenUtil jwtTokenUtil;

    // 使用构造函数注入依赖
    @Autowired
    public userService(userRepository userRepository,
                       WeChatApiUtil weChatApiUtil,
                       JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.weChatApiUtil = weChatApiUtil;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 这就是承接微信登录的核心方法
     * @param code 小程序 wx.login() 拿到的临时 code
     * @return 返回给小程序我们自己 App 的 JWT Token
     */
    public String loginWithWechat(String code) throws Exception {

        // 1. 调用微信 API，用 code 换取 UnionID
        // (weChatApiUtil 是您需要自己封装的工具类，它会请求 code2Session 接口)
        String unionId = weChatApiUtil.getUnionIdFromCode(code);
        if (unionId == null || unionId.isEmpty()) {
            throw new RuntimeException("微信登录失败，无法获取 UnionID");
        }

        // 2. 使用 UnionID 查询我们的数据库
        Optional<user> userOptional = userRepository.findByWechat_UnionID(unionId);

        user user;
        if (userOptional.isEmpty()) {
            // 3. 用户不存在 (新用户)，自动为他注册
            user = new user();
            user.setWechat_UnionID(unionId);
            user.setNickname("微信用户"); // 设置一个默认昵称
            user.setSignup_time(LocalDateTime.now()); // 设置注册时间
            user.set_ban(false); // 默认不封禁
            // 电话号码 等其他信息可以留空，等用户以后自己填

            user = userRepository.save(user); // 保存新用户到数据库
        } else {
            // 4. 用户已存在 (老用户)
            user = userOptional.get();
        }

        // 5. 检查封禁状态
        if (user.is_ban()) { //
            throw new RuntimeException("登录失败：该账户已被封禁");
        }

        // 6. 登录成功，生成我们自己的 JWT Token 并返回
        // Token 中应包含 user.getUserID() 作为身份标识
        String jwtToken = jwtTokenUtil.generateToken(user.getUserID().toString());

        return jwtToken;
    }
}