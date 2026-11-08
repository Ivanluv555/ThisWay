package org.daiso.cidi.modules.userfollow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;
import org.daiso.cidi.core.error.GlobalExceptionHandler;
import org.daiso.cidi.modules.userfollow.dto.UserFollowDTO;
import org.daiso.cidi.modules.userfollow.dto.UserStatsDTO;
import org.daiso.cidi.modules.userfollow.entity.UserFollow;
import org.daiso.cidi.modules.userfollow.service.UserFollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserFollowControllerTest {
    @Mock private UserFollowService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserFollowController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 验证关注操作路由和参数方向：输入关注者 1、目标 2，预期 200、返回关系且服务收到正确方向。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void followUserReturnsCreatedRelationship() throws Exception {
        when(service.followUser(1L, 2L)).thenReturn(UserFollow.builder().id(3L).followerId(1L).followedId(2L).build());
        mvc.perform(post("/api/user-follows/follow").param("followerId", "1").param("followedId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.followerId").value(1)).andExpect(jsonPath("$.followedId").value(2));
        verify(service).followUser(1L, 2L);
    }

    /**
     * 验证不能关注自己：输入两个数值相同但可能不是缓存对象的 Long 标识，预期 400 且不调用服务。
     *
     * @param userId 用于关注双方的相同用户标识
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(longs = {1L, 1000L})
    void followUserRejectsSelfFollow(long userId) throws Exception {
        mvc.perform(post("/api/user-follows/follow").param("followerId", String.valueOf(userId))
                        .param("followedId", String.valueOf(userId)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQ-400"))
                .andExpect(jsonPath("$.message").value("不能关注自己"));
        verifyNoInteractions(service);
    }

    /**
     * 验证取消关注成功返回无正文：输入有效用户对，预期按正确方向委托并返回 204。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void unfollowUserReturnsNoContent() throws Exception {
        mvc.perform(post("/api/user-follows/unfollow").param("followerId", "1").param("followedId", "2"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).unfollowUser(1L, 2L);
        verifyNoMoreInteractions(service);
    }

    /**
     * 验证关注状态接口返回布尔值而非关系实体：输入存在或不存在的关系状态，预期返回对应 JSON 布尔值。
     *
     * @param following 服务返回的关注状态
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isFollowingReturnsBoolean(boolean following) throws Exception {
        when(service.isFollowing(1L, 2L)).thenReturn(following);
        mvc.perform(get("/api/user-follows/is-following").param("followerId", "1").param("followedId", "2"))
                .andExpect(status().isOk()).andExpect(content().string(String.valueOf(following)));
        verify(service).isFollowing(1L, 2L);
    }

    /**
     * 验证关注列表支持匿名和登录访问者：输入有或无 currentUserId 的请求，预期准确透传并返回用户 DTO。
     *
     * @param authenticated 是否传递当前访问者标识
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getFollowingsPassesOptionalCurrentUser(boolean authenticated) throws Exception {
        Long viewer = authenticated ? 9L : null;
        when(service.getFollowings(1L, viewer)).thenReturn(List.of(UserFollowDTO.builder()
                .id(2L).userId(2L).username("关注对象").isFollowing(authenticated).isFollower(false).build()));
        var request = get("/api/user-follows/followings/1");
        if (authenticated) {
            request.param("currentUserId", "9");
        }
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].username").value("关注对象"))
                .andExpect(jsonPath("$[0].isFollowing").value(authenticated))
                .andExpect(jsonPath("$[0].isFollower").value(false));
        verify(service).getFollowings(1L, viewer);
    }

    /**
     * 验证粉丝列表支持可选访问者：输入有或无 currentUserId 的请求，预期调用粉丝查询而非关注查询。
     *
     * @param authenticated 是否传递当前访问者标识
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getFollowersPassesOptionalCurrentUser(boolean authenticated) throws Exception {
        Long viewer = authenticated ? 9L : null;
        when(service.getFollowers(1L, viewer)).thenReturn(List.of(UserFollowDTO.builder()
                .id(2L).userId(2L).username("粉丝").isFollowing(false).isFollower(authenticated).build()));
        var request = get("/api/user-follows/followers/1");
        if (authenticated) {
            request.param("currentUserId", "9");
        }
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("粉丝"))
                .andExpect(jsonPath("$[0].isFollowing").value(false))
                .andExpect(jsonPath("$[0].isFollower").value(authenticated));
        verify(service).getFollowers(1L, viewer);
        verifyNoMoreInteractions(service);
    }

    /**
     * 验证统计接口完整序列化：输入有或无访问者标识，预期返回用户标识、两种计数及双向关注状态。
     *
     * @param authenticated 是否传递当前访问者标识
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getStatsReturnsCountsAndRelationshipFlags(boolean authenticated) throws Exception {
        Long viewer = authenticated ? 9L : null;
        when(service.getUserStats(1L, viewer)).thenReturn(UserStatsDTO.builder().userId(1L)
                .followingCount(12L).followerCount(34L).isFollowing(authenticated).isFollower(false).build());
        var request = get("/api/user-follows/stats/1");
        if (authenticated) {
            request.param("currentUserId", "9");
        }
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.followingCount").value(12)).andExpect(jsonPath("$.followerCount").value(34))
                .andExpect(jsonPath("$.isFollowing").value(authenticated)).andExpect(jsonPath("$.isFollower").value(false));
        verify(service).getUserStats(1L, viewer);
    }

    /**
     * 验证空列表的 HTTP 表示：输入无关系的关注或粉丝列表，预期 200 和空数组。
     *
     * @param listType 查询的关系列表类型
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"followings", "followers"})
    void emptyRelationshipListsReturnEmptyArrays(String listType) throws Exception {
        if ("followings".equals(listType)) {
            when(service.getFollowings(1L, null)).thenReturn(List.of());
        } else {
            when(service.getFollowers(1L, null)).thenReturn(List.of());
        }
        mvc.perform(get("/api/user-follows/" + listType + "/1"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    /**
     * 验证关注失败不会被响应为成功：输入服务保存异常，预期标准 500 错误。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void followFailureReturnsServerError() throws Exception {
        when(service.followUser(1L, 2L)).thenThrow(new IllegalStateException("保存失败"));
        mvc.perform(post("/api/user-follows/follow").param("followerId", "1").param("followedId", "2"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("SYS-500"));
    }

    /**
     * 验证取消失败不会返回 204：输入取消关注服务异常，预期标准 500 错误。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void unfollowFailureReturnsServerError() throws Exception {
        doThrow(new IllegalStateException("取消失败")).when(service).unfollowUser(1L, 2L);
        mvc.perform(post("/api/user-follows/unfollow").param("followerId", "1").param("followedId", "2"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("SYS-500"));
    }

    /**
     * 记录缺少必填参数被错误分类的现有缺陷：输入缺失 followedId 的请求，当前通用异常处理器返回 500 而非客户端 400。
     * 预期本特征测试重现 SYS-500 且业务服务未执行；修复异常分类后应改成正确的 400 断言。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void knownDefectMissingFollowParameterReturnsServerError() throws Exception {
        mvc.perform(post("/api/user-follows/follow").param("followerId", "1"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("SYS-500"));
        verifyNoInteractions(service);
    }

    /**
     * 验证关系基础列表接口：输入两条关系，预期按服务顺序返回关系标识。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getAllReturnsRelationshipEntities() throws Exception {
        when(service.findAll()).thenReturn(List.of(UserFollow.builder().id(2L).build(), UserFollow.builder().id(1L).build()));
        mvc.perform(get("/api/user-follows")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2)).andExpect(jsonPath("$[1].id").value(1));
        verify(service).findAll();
    }

    /**
     * 验证按关系标识查询：输入存在的关系标识，预期返回 200 及关系双方标识。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void getByIdReturnsRelationship() throws Exception {
        when(service.findById(3L)).thenReturn(Optional.of(UserFollow.builder().id(3L).followerId(1L).followedId(2L).build()));
        mvc.perform(get("/api/user-follows/3")).andExpect(status().isOk())
                .andExpect(jsonPath("$.followerId").value(1)).andExpect(jsonPath("$.followedId").value(2));
    }

    /**
     * 验证基础创建接口的实体映射：输入关系双方标识，预期传给保存服务并返回分配后的关系标识。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void createMapsRelationshipBody() throws Exception {
        when(service.save(any())).thenReturn(UserFollow.builder().id(3L).followerId(1L).followedId(2L).build());
        mvc.perform(post("/api/user-follows").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followerId\":1,\"followedId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        ArgumentCaptor<UserFollow> captor = ArgumentCaptor.forClass(UserFollow.class);
        verify(service).save(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison()
                .isEqualTo(UserFollow.builder().followerId(1L).followedId(2L).build());
    }

    /**
     * 验证基础更新强制使用路径关系标识：输入正文标识 999、路径标识 3，预期检查存在后以 3 保存。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void updateUsesPathIdInsteadOfBodyId() throws Exception {
        when(service.findById(3L)).thenReturn(Optional.of(UserFollow.builder().id(3L).build()));
        when(service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(put("/api/user-follows/3").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"followerId\":1,\"followedId\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(3));
        ArgumentCaptor<UserFollow> captor = ArgumentCaptor.forClass(UserFollow.class);
        var order = inOrder(service);
        order.verify(service).findById(3L);
        order.verify(service).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(3L);
        assertThat(captor.getValue().getFollowerId()).isEqualTo(1L);
        assertThat(captor.getValue().getFollowedId()).isEqualTo(2L);
    }

    /**
     * 验证不存在关系的基础读改删边界：输入缺失关系标识，预期 404 且不执行保存或删除。
     *
     * @param method 访问缺失关系所用的 HTTP 方法
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingRelationshipReturnsNotFound(String method) throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());
        mvc.perform(request(HttpMethod.valueOf(method), "/api/user-follows/99")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"followerId\":1,\"followedId\":2}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REQ-404"))
                .andExpect(jsonPath("$.message").value("资源不存在: id=99"));
        verify(service).findById(99L);
        verifyNoMoreInteractions(service);
    }

    /**
     * 验证基础关系删除流程：输入存在的关系标识，预期先检查再删除并返回 204 无正文。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void deleteExistingRelationshipReturnsNoContent() throws Exception {
        when(service.findById(3L)).thenReturn(Optional.of(UserFollow.builder().id(3L).build()));
        mvc.perform(delete("/api/user-follows/3")).andExpect(status().isNoContent()).andExpect(content().string(""));
        var order = inOrder(service);
        order.verify(service).findById(3L);
        order.verify(service).deleteById(3L);
    }

    /**
     * 验证关系写接口拒绝损坏正文：输入残缺 JSON，预期 400 且不执行任何业务调用。
     *
     * @throws Exception 当模拟 HTTP 请求处理失败时抛出
     */
    @Test
    void createRejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/user-follows").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REQ-400"));
        verifyNoInteractions(service);
    }
}
