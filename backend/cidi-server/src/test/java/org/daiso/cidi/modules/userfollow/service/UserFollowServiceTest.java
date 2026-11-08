package org.daiso.cidi.modules.userfollow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.daiso.cidi.modules.user.entity.User;
import org.daiso.cidi.modules.user.repository.UserRepository;
import org.daiso.cidi.modules.userfollow.dto.UserFollowDTO;
import org.daiso.cidi.modules.userfollow.dto.UserStatsDTO;
import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.daiso.cidi.modules.userfollow.repository.UserFollowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {
    @Mock private UserFollowRepository repository;
    @Mock private UserRepository userRepository;
    @InjectMocks private UserFollowService service;

    /**
     * 验证首次关注建立正确方向的关系：输入关注者 1 和被关注者 2，预期保存方向正确、带当前时间的新关系。
     */
    @Test
    void followUserCreatesRelationshipWithTimestamp() {
        when(repository.findByFollowerIdAndFollowedId(1L, 2L)).thenReturn(Optional.empty());
        UserFollow saved = UserFollow.builder().id(10L).followerId(1L).followedId(2L).build();
        when(repository.save(any(UserFollow.class))).thenReturn(saved);
        LocalDateTime before = LocalDateTime.now();
        assertThat(service.followUser(1L, 2L)).isSameAs(saved);
        ArgumentCaptor<UserFollow> captor = ArgumentCaptor.forClass(UserFollow.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getFollowerId()).isEqualTo(1L);
        assertThat(captor.getValue().getFollowedId()).isEqualTo(2L);
        assertThat(captor.getValue().getCreateTime()).isBetween(before, LocalDateTime.now());
        var order = inOrder(repository);
        order.verify(repository).findByFollowerIdAndFollowedId(1L, 2L);
        order.verify(repository).save(captor.getValue());
    }

    /**
     * 验证重复关注的幂等性：输入已存在关系，预期直接返回原关系，不重复保存或刷新关注时间。
     */
    @Test
    void followUserReturnsExistingRelationshipWithoutSaving() {
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 0, 0);
        UserFollow existing = UserFollow.builder().id(10L).followerId(1L).followedId(2L).createTime(time).build();
        when(repository.findByFollowerIdAndFollowedId(1L, 2L)).thenReturn(Optional.of(existing));
        assertThat(service.followUser(1L, 2L)).isSameAs(existing);
        assertThat(existing.getCreateTime()).isEqualTo(time);
        verify(repository).findByFollowerIdAndFollowedId(1L, 2L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(userRepository);
    }

    /**
     * 验证关注查询失败后不写数据：输入仓储查询异常，预期原样传播且不调用保存。
     */
    @Test
    void followUserStopsAfterLookupFailure() {
        RuntimeException failure = new IllegalStateException("查询失败");
        when(repository.findByFollowerIdAndFollowedId(1L, 2L)).thenThrow(failure);
        assertThatThrownBy(() -> service.followUser(1L, 2L)).isSameAs(failure);
        verify(repository, never()).save(any());
    }

    /**
     * 验证关注保存失败不返回成功：输入不存在关系及保存异常，预期原样传播异常。
     */
    @Test
    void followUserPropagatesSaveFailure() {
        when(repository.findByFollowerIdAndFollowedId(1L, 2L)).thenReturn(Optional.empty());
        RuntimeException failure = new IllegalStateException("保存失败");
        when(repository.save(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.followUser(1L, 2L)).isSameAs(failure);
    }

    /**
     * 验证取消关注的方向和幂等委托：输入同一对用户两次，预期两次按相同方向删除，无额外存在性查询。
     */
    @Test
    void unfollowUserDelegatesDirectionalDeletionOnEveryRequest() {
        service.unfollowUser(1L, 2L);
        service.unfollowUser(1L, 2L);
        verify(repository, times(2)).deleteByFollowerIdAndFollowedId(1L, 2L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(userRepository);
    }

    /**
     * 验证取消关注错误可被感知：输入删除异常，预期原样传播而不是吞掉失败。
     */
    @Test
    void unfollowUserPropagatesFailure() {
        RuntimeException failure = new IllegalStateException("取消失败");
        doThrow(failure).when(repository).deleteByFollowerIdAndFollowedId(1L, 2L);
        assertThatThrownBy(() -> service.unfollowUser(1L, 2L)).isSameAs(failure);
    }

    /**
     * 验证关注状态由有向关系存在性决定：输入存在或不存在的关系，预期返回对应布尔值且不查询反向关系。
     *
     * @param exists 是否存在指定方向的关注关系
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isFollowingUsesDirectionalExistence(boolean exists) {
        when(repository.findByFollowerIdAndFollowedId(1L, 2L))
                .thenReturn(exists ? Optional.of(UserFollow.builder().id(5L).build()) : Optional.empty());
        assertThat(service.isFollowing(1L, 2L)).isEqualTo(exists);
        verify(repository).findByFollowerIdAndFollowedId(1L, 2L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证关注列表转换选取被关注者并保持顺序：输入两条不同时间关系和访问者，预期完整映射资料、时间及双向关系。
     */
    @Test
    void getFollowingsMapsFollowedUsersInRepositoryOrder() {
        LocalDateTime recent = LocalDateTime.of(2025, 3, 2, 10, 0);
        LocalDateTime old = recent.minusDays(1);
        when(repository.findFollowingsByUserId(1L)).thenReturn(List.of(
                UserFollow.builder().id(101L).followerId(1L).followedId(2L).createTime(recent).build(),
                UserFollow.builder().id(102L).followerId(1L).followedId(3L).createTime(old).build()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(profile(2L, "甲")));
        when(userRepository.findById(3L)).thenReturn(Optional.of(profile(3L, "乙")));
        when(repository.findByFollowerIdAndFollowedId(9L, 2L)).thenReturn(Optional.of(new UserFollow()));
        when(repository.findByFollowerIdAndFollowedId(2L, 9L)).thenReturn(Optional.empty());
        when(repository.findByFollowerIdAndFollowedId(9L, 3L)).thenReturn(Optional.empty());
        when(repository.findByFollowerIdAndFollowedId(3L, 9L)).thenReturn(Optional.of(new UserFollow()));

        List<UserFollowDTO> result = service.getFollowings(1L, 9L);

        assertThat(result).extracting(UserFollowDTO::getUserId).containsExactly(2L, 3L);
        assertThat(result.get(0)).usingRecursiveComparison().isEqualTo(UserFollowDTO.builder()
                .id(2L).userId(2L).username("甲").avatarUrl("avatar-2").bio("bio-2")
                .followTime(recent).isFollowing(true).isFollower(false).build());
        assertThat(result.get(1)).usingRecursiveComparison().isEqualTo(UserFollowDTO.builder()
                .id(3L).userId(3L).username("乙").avatarUrl("avatar-3").bio("bio-3")
                .followTime(old).isFollowing(false).isFollower(true).build());
        verify(userRepository, never()).findById(1L);
    }

    /**
     * 验证粉丝列表转换选取关注者：输入别人关注自己的关系，预期 DTO 使用粉丝标识并正确反映互相关注。
     */
    @Test
    void getFollowersMapsFollowerRatherThanFollowedUser() {
        LocalDateTime time = LocalDateTime.of(2025, 4, 1, 12, 0);
        when(repository.findFollowersByUserId(1L)).thenReturn(List.of(
                UserFollow.builder().id(101L).followerId(2L).followedId(1L).createTime(time).build()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(profile(2L, "粉丝")));
        when(repository.findByFollowerIdAndFollowedId(9L, 2L)).thenReturn(Optional.of(new UserFollow()));
        when(repository.findByFollowerIdAndFollowedId(2L, 9L)).thenReturn(Optional.of(new UserFollow()));
        assertThat(service.getFollowers(1L, 9L)).singleElement().satisfies(dto ->
                assertThat(dto).usingRecursiveComparison().isEqualTo(UserFollowDTO.builder()
                        .id(2L).userId(2L).username("粉丝").avatarUrl("avatar-2").bio("bio-2")
                        .followTime(time).isFollowing(true).isFollower(true).build()));
        verify(userRepository, never()).findById(1L);
    }

    /**
     * 验证匿名查看列表不查询私人关系状态：输入未提供访问者的关注或粉丝列表，预期两种关系标记均为假。
     *
     * @param followers 是否查询粉丝列表而非关注列表
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void anonymousListsSkipRelationshipLookups(boolean followers) {
        UserFollow relation = UserFollow.builder().followerId(2L).followedId(2L).build();
        if (followers) {
            when(repository.findFollowersByUserId(1L)).thenReturn(List.of(relation));
        } else {
            when(repository.findFollowingsByUserId(1L)).thenReturn(List.of(relation));
        }
        when(userRepository.findById(2L)).thenReturn(Optional.of(profile(2L, "匿名可见")));
        List<UserFollowDTO> result = followers ? service.getFollowers(1L, null) : service.getFollowings(1L, null);
        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getIsFollowing()).isFalse();
            assertThat(dto.getIsFollower()).isFalse();
        });
        verify(repository, never()).findByFollowerIdAndFollowedId(any(), any());
    }

    /**
     * 验证没有关系的用户列表边界：输入空关注或粉丝列表，预期返回空数组并跳过用户及关系补充查询。
     *
     * @param followers 是否查询粉丝列表
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void emptyListsDoNotLoadProfiles(boolean followers) {
        if (followers) {
            when(repository.findFollowersByUserId(1L)).thenReturn(List.of());
        } else {
            when(repository.findFollowingsByUserId(1L)).thenReturn(List.of());
        }
        assertThat(followers ? service.getFollowers(1L, 9L) : service.getFollowings(1L, 9L)).isEmpty();
        verifyNoInteractions(userRepository);
        verify(repository, never()).findByFollowerIdAndFollowedId(any(), any());
    }

    /**
     * 记录孤立关注记录的现有缺陷：输入关联用户已不存在的关系，当前结果含 null，而非过滤记录或报告资源错误。
     * 预期本特征测试重现 null 元素；这不是认可该输出，修复后应改为验证明确的业务处理策略。
     *
     * @param followers 是否测试粉丝列表入口
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void knownDefectMissingRelatedUserProducesNullListElement(boolean followers) {
        UserFollow relation = UserFollow.builder().followerId(2L).followedId(2L).build();
        if (followers) {
            when(repository.findFollowersByUserId(1L)).thenReturn(List.of(relation));
        } else {
            when(repository.findFollowingsByUserId(1L)).thenReturn(List.of(relation));
        }
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        List<UserFollowDTO> result = followers ? service.getFollowers(1L, 9L) : service.getFollowings(1L, 9L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
        verify(repository, never()).findByFollowerIdAndFollowedId(any(), any());
    }

    /**
     * 验证统计值及访问者关系独立计算：输入不同双向关注组合及大于整数上限的计数，预期不混淆方向或截断计数。
     *
     * @param following 当前访问者是否关注目标用户
     * @param follower 目标用户是否关注当前访问者
     */
    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void getUserStatsReturnsCountsAndIndependentDirections(boolean following, boolean follower) {
        when(repository.countFollowingsByUserId(1L)).thenReturn(3_000_000_000L);
        when(repository.countFollowersByUserId(1L)).thenReturn(4_000_000_000L);
        when(repository.findByFollowerIdAndFollowedId(9L, 1L))
                .thenReturn(following ? Optional.of(new UserFollow()) : Optional.empty());
        when(repository.findByFollowerIdAndFollowedId(1L, 9L))
                .thenReturn(follower ? Optional.of(new UserFollow()) : Optional.empty());
        UserStatsDTO stats = service.getUserStats(1L, 9L);
        assertThat(stats).usingRecursiveComparison().isEqualTo(UserStatsDTO.builder().userId(1L)
                .followingCount(3_000_000_000L).followerCount(4_000_000_000L)
                .isFollowing(following).isFollower(follower).build());
        verifyNoInteractions(userRepository);
    }

    /**
     * 验证匿名用户的零计数统计：输入零关注、零粉丝及空访问者，预期两项计数为零且无需关系查询。
     */
    @Test
    void anonymousStatsReturnZeroCountsWithoutRelationshipLookups() {
        when(repository.countFollowingsByUserId(1L)).thenReturn(0L);
        when(repository.countFollowersByUserId(1L)).thenReturn(0L);
        assertThat(service.getUserStats(1L, null)).usingRecursiveComparison().isEqualTo(UserStatsDTO.builder()
                .userId(1L).followingCount(0L).followerCount(0L).isFollowing(false).isFollower(false).build());
        verify(repository, never()).findByFollowerIdAndFollowedId(any(), any());
    }

    /**
     * 验证统计查询失败不返回不完整统计：输入计数异常，预期传播异常并跳过其余计数和关系查询。
     */
    @Test
    void statsPropagatesCountFailure() {
        RuntimeException failure = new IllegalStateException("统计失败");
        when(repository.countFollowingsByUserId(1L)).thenThrow(failure);
        assertThatThrownBy(() -> service.getUserStats(1L, 9L)).isSameAs(failure);
        verify(repository).countFollowingsByUserId(1L);
        verifyNoMoreInteractions(repository);
    }

    /**
     * 验证关注列表查资料故障不被当作用户不存在：输入用户仓储异常，预期原样传播而非返回 null 元素。
     */
    @Test
    void listPropagatesProfileLookupFailure() {
        when(repository.findFollowingsByUserId(1L)).thenReturn(List.of(
                UserFollow.builder().followerId(1L).followedId(2L).build()));
        RuntimeException failure = new IllegalStateException("资料查询失败");
        when(userRepository.findById(2L)).thenThrow(failure);
        assertThatThrownBy(() -> service.getFollowings(1L, null)).isSameAs(failure);
    }

    /**
     * 验证基础关系查询委托：输入一条仓储关系，预期列表及按标识查询均原样返回，不访问用户仓储。
     */
    @Test
    void basicReadsDelegateToRepository() {
        UserFollow follow = UserFollow.builder().id(3L).build();
        List<UserFollow> follows = List.of(follow);
        when(repository.findAll()).thenReturn(follows);
        when(repository.findById(3L)).thenReturn(Optional.of(follow));
        assertThat(service.findAll()).isSameAs(follows);
        assertThat(service.findById(3L)).containsSame(follow);
        verify(repository).findAll();
        verify(repository).findById(3L);
        verifyNoInteractions(userRepository);
    }

    /**
     * 验证基础查询的空边界：输入空列表及不存在标识，预期返回空集合和空 Optional。
     */
    @Test
    void basicReadsReturnEmptyResults() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findAll()).isEmpty();
        assertThat(service.findById(99L)).isEmpty();
    }

    /**
     * 验证基础保存和删除委托：输入关系实体及标识，预期返回仓储保存结果并按指定标识删除。
     */
    @Test
    void basicWritesDelegateToRepository() {
        UserFollow draft = UserFollow.builder().followerId(1L).followedId(2L).build();
        UserFollow saved = UserFollow.builder().id(3L).followerId(1L).followedId(2L).build();
        when(repository.save(draft)).thenReturn(saved);
        assertThat(service.save(draft)).isSameAs(saved);
        service.deleteById(3L);
        verify(repository).save(same(draft));
        verify(repository).deleteById(3L);
        verifyNoMoreInteractions(repository);
    }

    private User profile(Long id, String username) {
        return User.builder().id(id).username(username).avatarUrl("avatar-" + id).bio("bio-" + id).build();
    }
}
