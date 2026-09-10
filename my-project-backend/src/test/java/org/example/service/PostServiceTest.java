package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.entity.dto.Account;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.PostCreateVO;
import org.example.entity.vo.response.PageVO;
import org.example.entity.vo.response.PostDetailVO;
import org.example.entity.vo.response.PostListItemVO;
import org.example.mapper.PostMapper;
import org.example.mapper.FavoriteMapper;
import org.example.mapper.PostTombstoneMapper;
import org.example.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 帖子模块单元测试：mock 掉 MyBatis-Plus 的 Mapper 层，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostMapper postMapper;

    @Mock
    FavoriteMapper favoriteMapper;

    @Mock
    PostTombstoneMapper tombstoneMapper;

    @Mock
    AccountService accountService;

    @Mock
    StringRedisTemplate stringRedisTemplate;


    @Mock
    ValueOperations<String, String> operations;

    @Spy
    @InjectMocks
    PostServiceImpl service;

    private PostCreateVO validVO() {
        PostCreateVO vo = new PostCreateVO();
        vo.setTitle("一个资源分享");
        vo.setContent("这是正文内容");
        vo.setResourceUrl("https://example.com/file.zip");
        return vo;
    }

    private Account account(int id, String nickname) {
        return new Account(id, "user" + id, "password", "u" + id + "@mail.com", "user", new Date(), nickname, null);
    }

    @BeforeEach
    void setUp() {
        lenient().doReturn(true).when(service).save(any(Post.class));
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(operations);
        lenient().when(operations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(operations.increment(anyString())).thenReturn(1L);
        lenient().when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    void createPost_success_returnsNullMessage() {
        String message = service.createPost(1, validVO());
        assertNull(message, "发布成功应返回 null");
        verify(service).save(argThat(post ->
                post.getAuthorId() == 1
                        && "一个资源分享".equals(post.getTitle())
                        && "https://example.com/file.zip".equals(post.getResourceUrl())
                        && post.getLikeCount() == 0
                        && post.getCommentCount() == 0));
    }

    @Test
    void createPost_saveFails_returnsErrorMessage() {
        doReturn(false).when(service).save(any(Post.class));
        String message = service.createPost(1, validVO());
        assertEquals("内部错误，请联系管理员", message);
    }

    @Test
    void createPost_intervalLimitRejected() {
        when(operations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        String message = service.createPost(1, validVO());

        assertEquals("30秒内不能重复发帖，请稍后再试", message);
        verify(service, never()).save(any(Post.class));
    }

    @Test
    void createPost_dailyLimitRejected() {
        when(operations.increment(anyString())).thenReturn(101L);

        String message = service.createPost(1, validVO());

        assertEquals("今日发帖已达上限（每天最多发布50帖）", message);
        verify(service, never()).save(any(Post.class));
    }

    @Test
    void listPosts_mapsEntityToVOWithNickname() {
        Page<Post> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1);
        Post post = new Post(7, 2, "标题", "正文", "https://example.com", 5, 3, new Date(), new Date());
        pageResult.setRecords(List.of(post));
        doReturn(pageResult).when(service).page(any(Page.class), any());
        when(accountService.listByIds(anyList())).thenReturn(List.of(account(2, "小明")));

        PageVO<PostListItemVO> vo = service.listPosts(1, 10, "latest");

        assertEquals(1, vo.total());
        assertEquals(1, vo.items().size());
        PostListItemVO item = vo.items().get(0);
        assertEquals(7, item.id());
        assertEquals("小明", item.authorNickname(), "列表项应带作者昵称");
        assertEquals(5, item.likeCount());
    }

    @Test
    void listPosts_unknownAuthor_fallsBackToPlaceholder() {
        Page<Post> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of(new Post(7, 99, "标题", "正文", "https://example.com", 0, 0, new Date(), new Date())));
        doReturn(pageResult).when(service).page(any(Page.class), any());
        when(accountService.listByIds(anyList())).thenReturn(List.of());

        PageVO<PostListItemVO> vo = service.listPosts(1, 10, "latest");

        assertEquals("未知用户", vo.items().get(0).authorNickname());
    }

    @Test
    void getPostDetail_exists_returnsDetailWithNickname() {
        Post post = new Post(7, 2, "标题", "正文", "https://example.com", 5, 3, new Date(), new Date());
        doReturn(post).when(service).getById(7);
        when(accountService.findAccountById(2)).thenReturn(Optional.of(account(2, "小红")));

        PostDetailVO vo = service.getPostDetail(7);

        assertNotNull(vo);
        assertEquals("小红", vo.authorNickname());
        assertEquals(3, vo.commentCount());
        verify(operations).set(eq("post:detail:7"), anyString(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getPostDetail_cacheHit_skipsDatabase() throws Exception {
        PostDetailVO cached = new PostDetailVO(7, 2, "小红", "标题", "正文", "https://example.com", 5, 3, new Date(), new Date());
        when(operations.get("post:detail:7")).thenReturn(serialize(cached));

        PostDetailVO result = service.getPostDetail(7);

        assertEquals(cached, result);
        verify(service, never()).getById(7);
    }

    @Test
    void getPostDetail_notExists_returnsNull() {
        doReturn(null).when(service).getById(404);
        assertNull(service.getPostDetail(404));
    }

    @Test
    void deletePost_physicallyRemovesOwnedPost() {
        Post post = new Post(7, 1, "标题", "正文", "https://example.com", 0, 0, new Date(), new Date());
        doReturn(post).when(service).getById(7);
        doReturn(true).when(service).removeById(7);

        assertNull(service.deletePost(1, 7));
        verify(service).removeById(7);
        verify(stringRedisTemplate).delete("post:detail:7");
    }

    private String serialize(PostDetailVO detail) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(detail);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
