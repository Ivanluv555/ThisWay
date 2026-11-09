package org.daiso.cidi.modules.user.repository;

import org.daiso.cidi.modules.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 小程序用户资料的 Spring Data JPA 数据访问接口。
 *
 * <p>继承 {@link JpaRepository} 提供的主键查询与增删改操作；主键类型为 {@code Long}。
 *
 * <p>额外按唯一 openid 查找登录用户、按手机号查找绑定归属，供微信认证服务使用。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByOpenid(String openid);

	Optional<User> findByPhone(String phone);
}
