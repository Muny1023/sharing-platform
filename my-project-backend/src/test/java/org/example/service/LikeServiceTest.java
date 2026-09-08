package org.example.service;

import org.example.entity.dto.Comment;
import org.example.entity.dto.Like;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.LikeToggleVO;
import org.example.mapper.LikeMapper;
import org.example.service.impl.LikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 点赞模块单元测试：mock 掉 Mapper 与关联 Service，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    LikeMapper likeMapper;

    @Mock
    PostService postService;

    @Mock
    CommentService commentService;

    @Spy
    @InjectMocks
    LikeServiceImpl service;

    private LikeToggleVO vo(String targetType, Integer targetId) {
        LikeToggleVO vo = new LikeToggleVO();
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        return vo;
    }

    @BeforeEach
    void setUp() {
        lenient().doReturn(true).when(service).save(any(Like.class));
        lenient().doReturn(true).when(service).removeById(any(java.io.Serializable.class));
        lenient().when(postService.update()).thenReturn(new PostChainStub());
        lenient().when(commentService.update()).thenReturn(new CommentChainStub());
    }

    @Test
    void toggleLike_likePost_insertsRecordAndIncrements() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 0, 0, new Date(), new Date()));
        doReturn(null).when(service).getOne(any());
        PostChainStub chain = new PostChainStub();
        when(postService.update()).thenReturn(chain);

        LikeService.LikeToggleResult result = service.toggleLike(1, vo("post", 7));

        assertNull(result.message());
        assertTrue(result.liked(), "首次点赞后应处于已赞状态");
        verify(service).save(argThat(like -> like.getPostId() == 7 && like.getCommentId() == null && like.getUserId() == 1));
        assertTrue(chain.executed, "点赞后应更新帖子 like_count");
    }

    @Test
    void toggleLike_likeAgain_removesRecordAndDecrements() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 1, 0, new Date(), new Date()));
        doReturn(new Like(5, 1, 7, null, new Date())).when(service).getOne(any());
        PostChainStub chain = new PostChainStub();
        when(postService.update()).thenReturn(chain);

        LikeService.LikeToggleResult result = service.toggleLike(1, vo("post", 7));

        assertNull(result.message());
        assertFalse(result.liked(), "已赞再点应取消");
        verify(service).removeById((java.io.Serializable) 5);
        assertTrue(chain.executed, "取消赞后应更新帖子 like_count");
    }

    @Test
    void toggleLike_commentTarget_works() {
        when(commentService.getById(9)).thenReturn(new Comment(9, 7, 2, null, "内容", 0, new Date(), new Date()));
        doReturn(null).when(service).getOne(any());
        CommentChainStub chain = new CommentChainStub();
        when(commentService.update()).thenReturn(chain);

        LikeService.LikeToggleResult result = service.toggleLike(1, vo("comment", 9));

        assertNull(result.message());
        assertTrue(result.liked());
        verify(service).save(argThat(like -> like.getCommentId() == 9 && like.getPostId() == null));
        assertTrue(chain.executed, "评论点赞后应更新评论 like_count");
    }

    @Test
    void toggleLike_invalidType_rejected() {
        LikeService.LikeToggleResult result = service.toggleLike(1, vo("video", 7));

        assertEquals("目标类型只能是 post 或 comment", result.message());
        verify(service, never()).save(any(Like.class));
    }

    @Test
    void toggleLike_postNotExists_rejected() {
        when(postService.getById(404)).thenReturn(null);
        assertEquals("帖子不存在或已被删除", service.toggleLike(1, vo("post", 404)).message());
        verify(service, never()).save(any(Like.class));
    }

    @Test
    void toggleLike_commentNotExists_rejected() {
        when(commentService.getById(404)).thenReturn(null);
        assertEquals("评论不存在或已被删除", service.toggleLike(1, vo("comment", 404)).message());
        verify(service, never()).save(any(Like.class));
    }

    @Test
    void findLikedTargetIds_postType_returnsLikedPostIds() {
        doReturn(List.of(new Like(5, 1, 7, null, new Date()), new Like(6, 1, 9, null, new Date())))
                .when(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        assertEquals(Set.of(7, 9), service.findLikedTargetIds(1, "post", List.of(7, 8, 9)));
    }

    @Test
    void findLikedTargetIds_commentType_returnsLikedCommentIds() {
        doReturn(List.of(new Like(5, 1, null, 3, new Date())))
                .when(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        assertEquals(Set.of(3), service.findLikedTargetIds(1, "comment", List.of(2, 3, 4)));
    }

    @Test
    void findLikedTargetIds_emptyInput_returnsEmptyWithoutQuery() {
        assertEquals(Set.of(), service.findLikedTargetIds(1, "post", List.of()));
        verify(service, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 代替 MyBatis-Plus 的 update() 链式调用，只记录是否触发了计数更新。
     */
    static class PostChainStub extends com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Post> {
        boolean executed = false;
        public PostChainStub() { super((com.baomidou.mybatisplus.core.mapper.BaseMapper<Post>) null); }
        public com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Post> eq(String column, Object val) { return this; }
        public com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Post> setSql(String sql) { return this; }
        public boolean update() { executed = true; return true; }
    }

    static class CommentChainStub extends com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Comment> {
        boolean executed = false;
        public CommentChainStub() { super((com.baomidou.mybatisplus.core.mapper.BaseMapper<Comment>) null); }
        public com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Comment> eq(String column, Object val) { return this; }
        public com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Comment> setSql(String sql) { return this; }
        public boolean update() { executed = true; return true; }
    }
}
