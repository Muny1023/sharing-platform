package org.example.service;

import org.example.entity.dto.Comment;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.CommentCreateVO;
import org.example.entity.vo.response.CommentVO;
import org.example.mapper.CommentMapper;
import org.example.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 评论模块单元测试：mock 掉 Mapper 与关联 Service，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    CommentMapper commentMapper;

    @Mock
    PostService postService;

    @Mock
    AccountService accountService;

    @Spy
    @InjectMocks
    CommentServiceImpl service;

    private CommentCreateVO vo(Integer postId, Integer parentId, String content) {
        CommentCreateVO vo = new CommentCreateVO();
        vo.setPostId(postId);
        vo.setParentId(parentId);
        vo.setContent(content);
        return vo;
    }

    private Comment comment(int id, int postId, int authorId, Integer parentId, Date time) {
        return new Comment(id, postId, authorId, parentId, "内容" + id, 0, time, time);
    }

    @BeforeEach
    void setUp() {
        lenient().doReturn(true).when(service).save(any(Comment.class));
        // 计数更新走 postService 的链式调用，stub 掉避免真实 SQL
        lenient().when(postService.update()).thenReturn(new PostUpdateChainStub());
    }

    @Test
    void createComment_topLevel_success_incrementsPostCounter() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 0, 0, new Date(), new Date()));
        PostUpdateChainStub chain = new PostUpdateChainStub();
        when(postService.update()).thenReturn(chain);

        String message = service.createComment(1, vo(7, null, "一条顶级评论"));

        assertNull(message);
        verify(service).save(argThat(c -> c.getPostId() == 7 && c.getParentId() == null && c.getAuthorId() == 1));
        assertTrue(chain.executed, "发布评论后应更新帖子的 comment_count");
    }

    @Test
    void createComment_postNotExists_returnsError() {
        when(postService.getById(404)).thenReturn(null);
        assertEquals("帖子不存在或已被删除", service.createComment(1, vo(404, null, "内容")));
        verify(service, never()).save(any());
    }

    @Test
    void createComment_replyToTopComment_success() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 0, 1, new Date(), new Date()));
        doReturn(comment(3, 7, 2, null, new Date())).when(service).getById(3);

        String message = service.createComment(1, vo(7, 3, "一条回复"));

        assertNull(message);
        verify(service).save(argThat(c -> c.getParentId() == 3));
    }

    @Test
    void createComment_replyToReply_rejected() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 0, 2, new Date(), new Date()));
        doReturn(comment(4, 7, 2, 3, new Date())).when(service).getById(4); // parentId=3，本身已是回复

        assertEquals("最多只能回复一级评论", service.createComment(1, vo(7, 4, "楼中楼")));
        verify(service, never()).save(any());
    }

    @Test
    void createComment_replyToMissingParent_rejected() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 0, 0, new Date(), new Date()));
        doReturn(null).when(service).getById(99);

        assertEquals("要回复的评论不存在或已被删除", service.createComment(1, vo(7, 99, "回复空气")));
        verify(service, never()).save(any());
    }

    @Test
    void createComment_parentFromOtherPost_rejected() {
        when(postService.getById(7)).thenReturn(new Post(7, 2, "标题", "正文", "https://example.com", 0, 0, new Date(), new Date()));
        doReturn(comment(3, 8, 2, null, new Date())).when(service).getById(3); // 属于帖子 8

        assertEquals("回复目标与帖子不匹配", service.createComment(1, vo(7, 3, "跨帖回复")));
        verify(service, never()).save(any());
    }

    @Test
    void listComments_buildsTwoLevelTree() {
        Date t1 = new Date(1000), t2 = new Date(2000), t3 = new Date(3000), t4 = new Date(4000);
        doReturn(new CommentQueryStub(List.of(
                comment(1, 7, 10, null, t1),
                comment(2, 7, 20, null, t2),
                comment(3, 7, 10, 1, t3),
                comment(4, 7, 20, 1, t4)
        ))).when(service).query();

        List<CommentVO> tree = service.listCommentsByPost(7);

        assertEquals(2, tree.size(), "两条顶级评论");
        assertEquals(1, tree.get(0).id());
        assertEquals(2, tree.get(1).id());
        assertEquals(2, tree.get(0).replies().size(), "顶级评论1下挂两条回复");
        assertEquals(3, tree.get(0).replies().get(0).id());
        assertEquals(4, tree.get(0).replies().get(1).id());
        assertTrue(tree.get(1).replies().isEmpty());
    }

    @Test
    void listComments_empty_returnsEmptyList() {
        doReturn(new CommentQueryStub(List.of())).when(service).query();
        assertTrue(service.listCommentsByPost(7).isEmpty());
        verify(accountService, never()).listByIds(anyList());
    }

    /**
     * 代替 MyBatis-Plus 的 query() 链式调用，忽略条件、直接返回预设列表。
     */
    static class CommentQueryStub extends com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper<Comment> {
        private final List<Comment> result;
        public CommentQueryStub(List<Comment> result) {
            super((com.baomidou.mybatisplus.core.mapper.BaseMapper<Comment>) null);
            this.result = result;
        }
        public com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper<Comment> eq(String column, Object val) { return this; }
        public com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper<Comment> orderByAsc(String column) { return this; }
        public List<Comment> list() { return result; }
    }

    /**
     * 代替 MyBatis-Plus 的 update() 链式调用，只记录是否触发了计数更新。
     */
    static class PostUpdateChainStub extends com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Post> {
        boolean executed = false;
        public PostUpdateChainStub() { super((com.baomidou.mybatisplus.core.mapper.BaseMapper<Post>) null); }
        public com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Post> eq(String column, Object val) { return this; }
        public com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper<Post> setSql(String sql) { return this; }
        public boolean update() { executed = true; return true; }
    }
}
