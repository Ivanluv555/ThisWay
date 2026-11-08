package org.daiso.cidi.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository repository;
    @InjectMocks private UserService service;

    /**
     * 验证用户列表查询不丢失仓储顺序：输入两名用户，预期原样返回列表且只查询一次。
     */
    @Test
    void findAllReturnsRepositoryUsers() {
        List<User> users = List.of(User.builder().id(2L).build(), User.builder().id(1L).build());
        when(repository.findAll()).thenReturn(users);
        assertThat(service.findAll()).isSameAs(users);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证无用户时的列表边界：输入空仓储列表，预期返回空列表而非空引用。
     */
    @Test
    void findAllReturnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    /**
     * 验证按标识查找用户：输入存在的用户标识，预期返回相同实体且不写入数据。
     */
    @Test
    void findByIdReturnsExistingUser() {
        User user = User.builder().id(4L).username("小李").build();
        when(repository.findById(4L)).thenReturn(Optional.of(user));
        assertThat(service.findById(4L)).containsSame(user);
        verify(repository).findById(4L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证不存在用户的查询语义：输入未登记标识，预期返回空 Optional 供控制器判断。
     */
    @Test
    void findByIdReturnsEmptyForMissingUser() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThat(service.findById(9L)).isEmpty();
    }

    /**
     * 验证用户保存结果以仓储为准：输入未分配标识的新用户，预期返回仓储分配标识后的实体。
     */
    @Test
    void saveReturnsPersistedEntity() {
        User draft = User.builder().username("新用户").openid("open-1").build();
        User saved = User.builder().id(5L).username("新用户").openid("open-1").build();
        when(repository.save(draft)).thenReturn(saved);
        assertThat(service.save(draft)).isSameAs(saved);
        verify(repository).save(same(draft));
    }

    /**
     * 验证删除委托不擅自增加查询：输入用户标识，预期只调用该标识的删除操作。
     */
    @Test
    void deleteByIdDelegatesOnlyDeletion() {
        service.deleteById(5L);
        verify(repository).deleteById(5L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证保存失败不会被伪装成成功：输入仓储拒绝的用户，预期原样传播异常。
     */
    @Test
    void savePropagatesRepositoryFailure() {
        User user = User.builder().openid("duplicate").build();
        RuntimeException failure = new IllegalStateException("唯一约束冲突");
        when(repository.save(user)).thenThrow(failure);
        assertThatThrownBy(() -> service.save(user)).isSameAs(failure);
    }

    /**
     * 验证查询故障不会降级为空列表：输入仓储查询异常，预期原样传播异常。
     */
    @Test
    void findAllPropagatesRepositoryFailure() {
        RuntimeException failure = new IllegalStateException("查询失败");
        when(repository.findAll()).thenThrow(failure);
        assertThatThrownBy(service::findAll).isSameAs(failure);
    }

    /**
     * 验证删除失败可被上层感知：输入删除异常，预期异常原样传播且没有后续仓储操作。
     */
    @Test
    void deletePropagatesRepositoryFailure() {
        RuntimeException failure = new IllegalStateException("删除失败");
        doThrow(failure).when(repository).deleteById(5L);
        assertThatThrownBy(() -> service.deleteById(5L)).isSameAs(failure);
        verify(repository).deleteById(5L);
        verifyNoMoreInteractions(repository);
    }
}
