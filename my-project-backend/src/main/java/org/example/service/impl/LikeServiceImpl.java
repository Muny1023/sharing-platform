package org.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Comment;
import org.example.entity.dto.Like;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.LikeToggleVO;
import org.example.mapper.LikeMapper;
import org.example.service.CommentService;
import org.example.service.LikeService;
import org.example.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class LikeServiceImpl extends ServiceImpl<LikeMapper, Like> implements LikeService {

    @Resource
    PostService postService;

    @Resource
    CommentService commentService;
    @Resource
    AmqpTemplate amqpTemplate;

    @Override
    @Transactional
    public LikeToggleResult toggleLike(Integer userId, LikeToggleVO vo) {
        boolean onPost = "post".equals(vo.getTargetType());
        boolean onComment = "comment".equals(vo.getTargetType());
        if (!onPost && !onComment) return LikeToggleResult.failure("目标类型只能是 post 或 comment");

        // 校验目标存在
        if (onPost) {
            if (postService.getById(vo.getTargetId()) == null || Boolean.TRUE.equals(postService.getById(vo.getTargetId()).getDeleted())) return LikeToggleResult.failure("帖子不存在或已被删除");
        } else {
            if (commentService.getById(vo.getTargetId()) == null) return LikeToggleResult.failure("评论不存在或已被删除");
        }

        // 幂等切换：已有记录则取消（删记录、计数-1），没有则点赞（插记录、计数+1）
        Like existing = this.getOne(Wrappers.<Like>query()
                .eq("user_id", userId)
                .eq(onPost ? "post_id" : "comment_id", vo.getTargetId()));

        if (existing != null) {
            if (!this.removeById(existing.getId())) return LikeToggleResult.failure("内部错误，请联系管理员");
            return adjustCount(onPost, vo.getTargetId(), -1)
                    ? LikeToggleResult.success(false, 0)
                    : LikeToggleResult.failure("内部错误，请联系管理员");
        }

        Like like = onPost
                ? new Like(null, userId, vo.getTargetId(), null, new Date())
                : new Like(null, userId, null, vo.getTargetId(), new Date());
        if (!this.save(like)) return LikeToggleResult.failure("内部错误，请联系管理员");
        boolean adjusted = adjustCount(onPost, vo.getTargetId(), 1);
        if (adjusted && onPost && amqpTemplate != null) {
            Post post = postService.getById(vo.getTargetId());
            if (post != null && !userId.equals(post.getAuthorId())) {
                amqpTemplate.convertAndSend("notification", new org.example.service.NotificationService.NotificationEvent(
                        UUID.randomUUID().toString(), userId, post.getAuthorId(), "POST_LIKE", post.getId(), null, "有人赞了你的帖子"));
            }
        }
        return adjusted
                ? LikeToggleResult.success(true, 1)
                : LikeToggleResult.failure("内部错误，请联系管理员");
    }

    /**
     * 调整目标冗余计数。链式接口拿不到更新后的值，MVP 阶段前端切完后从列表/详情重新拉取。
     */
    private boolean adjustCount(boolean onPost, Integer targetId, int delta) {
        return onPost
                ? postService.update().eq("id", targetId).setSql("like_count = like_count + " + delta).update()
                : commentService.update().eq("id", targetId).setSql("like_count = like_count + " + delta).update();
    }

    @Override
    public Set<Integer> findLikedTargetIds(Integer userId, String targetType, List<Integer> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) return Set.of();
        String column = "post".equals(targetType) ? "post_id" : "comment_id";
        return this.list(Wrappers.<Like>query()
                        .eq("user_id", userId)
                        .in(column, targetIds))
                .stream()
                .map(like -> "post".equals(targetType) ? like.getPostId() : like.getCommentId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
