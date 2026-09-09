package org.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Account;
import org.example.entity.dto.Comment;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.CommentCreateVO;
import org.example.entity.vo.response.CommentVO;
import org.example.mapper.CommentMapper;
import org.example.service.AccountService;
import org.example.service.CommentService;
import org.example.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.core.AmqpTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    PostService postService;

    @Resource
    AccountService accountService;
    @Resource
    AmqpTemplate amqpTemplate;

    @Override
    @Transactional
    public String createComment(Integer authorId, CommentCreateVO vo) {
        Post post = postService.getById(vo.getPostId());
        if (post == null) return "帖子不存在或已被删除";

        // 两级评论约束：回复只能挂在顶级评论（parentId 为 null 的评论）下
        if (vo.getParentId() != null) {
            Comment parent = this.getById(vo.getParentId());
            if (parent == null) return "要回复的评论不存在或已被删除";
            if (parent.getParentId() != null) return "最多只能回复一级评论";
            if (!parent.getPostId().equals(vo.getPostId())) return "回复目标与帖子不匹配";
        }

        Comment comment = new Comment(null, vo.getPostId(), authorId, vo.getParentId(), vo.getContent(), 0, new Date(), new Date());
        if (!this.save(comment)) return "内部错误，请联系管理员";
        // 冗余计数 +1，与评论同事务
        postService.update().eq("id", vo.getPostId()).setSql("comment_count = comment_count + 1").update();
        Integer recipientId;
        String type;
        Integer commentId = comment.getId();
        if (vo.getParentId() == null) {
            recipientId = post.getAuthorId();
            type = "POST_COMMENT";
        } else {
            Comment parent = this.getById(vo.getParentId());
            recipientId = parent == null ? null : parent.getAuthorId();
            type = "COMMENT_REPLY";
        }
        if (recipientId != null && !recipientId.equals(authorId) && amqpTemplate != null) {
            amqpTemplate.convertAndSend("notification", new org.example.service.NotificationService.NotificationEvent(
                    UUID.randomUUID().toString(), authorId, recipientId, type, vo.getPostId(), commentId,
                    vo.getParentId() == null ? "评论了你的帖子" : "回复了你的评论"));
        }
        return null;
    }

    @Override
    public List<CommentVO> listCommentsByPost(Integer postId) {
        List<Comment> comments = this.query()
                .eq("post_id", postId)
                .orderByAsc("create_time")
                .list();
        if (comments.isEmpty()) return List.of();

        Map<Integer, String> nicknameMap = accountService.listByIds(
                        comments.stream().map(Comment::getAuthorId).distinct().toList()).stream()
                .collect(Collectors.toMap(Account::getId, Account::getNickname, (a, b) -> a));

        // 先转 VO，再按 parentId 归组：顶级评论保序，回复挂到对应顶级评论下
        Map<Integer, CommentVO> topMap = new LinkedHashMap<>();
        Map<Integer, List<CommentVO>> replyMap = new LinkedHashMap<>();
        for (Comment c : comments) {
            CommentVO vo = new CommentVO(c.getId(), c.getPostId(), c.getAuthorId(),
                    nicknameMap.getOrDefault(c.getAuthorId(), "未知用户"),
                    c.getParentId(), c.getContent(), c.getLikeCount(), c.getCreateTime(), new ArrayList<>());
            if (c.getParentId() == null) {
                topMap.put(c.getId(), vo);
            } else {
                replyMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(vo);
            }
        }
        topMap.forEach((topId, topVO) -> topVO.replies().addAll(replyMap.getOrDefault(topId, List.of())));
        return new ArrayList<>(topMap.values());
    }
}
