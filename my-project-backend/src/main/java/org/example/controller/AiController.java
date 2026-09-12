package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.vo.request.AiChatRequestVO;
import org.example.entity.vo.request.AiSearchRequestVO;
import org.example.entity.vo.response.AiChatVO;
import org.example.entity.vo.response.PageVO;
import org.example.entity.vo.response.PostListItemVO;
import org.example.service.AiService;
import org.example.service.PostService;
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

    @Value("${ai.service-token}")
    String serviceToken;

    @PostMapping("/api/ai/chat")
    public ResponseEntity<RestBean<AiChatVO>> chat(@RequestBody @Valid AiChatRequestVO request,
                                                    @RequestAttribute("id") Integer userId) {
        try {
            return ResponseEntity.ok(RestBean.success(
                    aiService.chat(userId, request.conversationId(), request.message())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(RestBean.failure(400, e.getMessage()));
        } catch (AiService.AiServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(RestBean.failure(503, e.getMessage()));
        }
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
