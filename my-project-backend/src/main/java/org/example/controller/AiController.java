package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.vo.request.AiChatRequestVO;
import org.example.entity.vo.request.AiSearchRequestVO;
import org.example.entity.vo.request.AiPostContentRequestVO;
import org.example.entity.vo.response.AiChatVO;
import org.example.entity.vo.response.AiPostContentVO;
import org.example.entity.vo.response.PageVO;
import org.example.entity.vo.response.PostListItemVO;
import org.example.entity.vo.request.AiPostFeedbackRequestVO;
import org.example.entity.vo.response.AiCommentItemVO;
import org.example.entity.vo.response.AiPostFeedbackVO;
import org.example.service.CommentService;
import org.example.service.AiService;
import org.example.service.PostService;
import org.example.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
public class AiController {

    @Resource
    AiService aiService;

    @Resource
    PostService postService;
    @Resource CommentService commentService;
    @Resource NotificationService notificationService;

    @Value("${ai.service-token}")
    String serviceToken;

    @PostMapping("/api/ai/chat")
    public ResponseEntity<RestBean<AiChatVO>> chat(@RequestBody @Valid AiChatRequestVO request,
                                                    @RequestAttribute("id") Integer userId) {
        try {
            return ResponseEntity.ok(RestBean.success(
                    aiService.chat(userId, request.conversationId(), request.targetPostId(), request.feedbackPostId(), request.draftPostId(), Boolean.TRUE.equals(request.unreadOnly()), request.message())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(RestBean.failure(400, e.getMessage()));
        } catch (AiService.AiServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(RestBean.failure(503, e.getMessage()));
        }
    }

    @PostMapping("/internal/ai/post-content")
    public ResponseEntity<RestBean<AiPostContentVO>> postContent(
            @RequestHeader(value = "X-AI-Service-Token", required = false) String token,
            @RequestBody @Valid AiPostContentRequestVO request) {
        if (!validToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RestBean.failure(401, "无效的服务凭证"));
        }
        var post = postService.getById(request.postId());
        if (post == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(RestBean.failure(404, "帖子不存在"));
        }
        if (Boolean.TRUE.equals(post.getDeleted())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(RestBean.failure(410, "该帖子已被作者删除"));
        }
        return ResponseEntity.ok(RestBean.success(
                new AiPostContentVO(post.getId(), post.getTitle(), post.getContent())));
    }

    @PostMapping("/internal/ai/post-feedback")
    public ResponseEntity<RestBean<AiPostFeedbackVO>> postFeedback(
            @RequestHeader(value = "X-AI-Service-Token", required = false) String token,
            @RequestBody @Valid AiPostFeedbackRequestVO request) {
        if (!validToken(token)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RestBean.failure(401, "无效的服务凭证"));
        var post = postService.getById(request.postId());
        if (post == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RestBean.failure(404, "帖子不存在"));
        if (Boolean.TRUE.equals(post.getDeleted())) return ResponseEntity.status(HttpStatus.GONE).body(RestBean.failure(410, "该帖子已被作者删除"));
        if (!request.userId().equals(post.getAuthorId())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RestBean.failure(403, "只能分析自己发布的帖子"));
        var comments = commentService.listActiveCommentsByPost(post.getId());
        if (request.unreadOnly()) {
            var unreadIds = new java.util.HashSet<>(notificationService.listUnreadCommentIds(request.userId(), post.getId()));
            comments = comments.stream().filter(c -> unreadIds.contains(c.getId())).toList();
        }
        boolean partial = comments.size() > 100;
        var selected = comments.stream().skip(Math.max(0, comments.size() - 100L)).map(c -> new AiCommentItemVO(c.getId(), c.getParentId(), c.getContent(), c.getCreateTime())).toList();
        return ResponseEntity.ok(RestBean.success(new AiPostFeedbackVO(post.getId(), post.getContent(), post.getUpdateTime(), selected, partial)));
    }

    @PostMapping("/internal/ai/search")
    public ResponseEntity<RestBean<PageVO<PostListItemVO>>> search(
            @RequestHeader(value = "X-AI-Service-Token", required = false) String token,
            @RequestBody @Valid AiSearchRequestVO request) {
        if (!validToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RestBean.failure(401, "无效的服务凭证"));
        }
        return ResponseEntity.ok(RestBean.success(
                postService.searchPosts(request.keyword() == null ? "" : request.keyword().trim(), request.page(), request.size())));
    }

    private boolean validToken(String token) {
        return token != null && MessageDigest.isEqual(
                serviceToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }
}
