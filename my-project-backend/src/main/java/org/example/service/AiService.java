package org.example.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.example.entity.dto.Post;
import org.example.entity.vo.response.AiChatVO;
import org.example.entity.vo.response.AiPostCardVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${ai.service-url}")
    String serviceUrl;

    @Value("${ai.service-token}")
    String serviceToken;

    @Resource
    PostService postService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public AiChatVO chat(Integer userId, String conversationId, Integer targetPostId, Integer feedbackPostId, Integer draftPostId, boolean unreadOnly, String message) {
        String currentConversationId = normalizeConversationId(conversationId);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("conversationId", currentConversationId);
            body.put("message", message.trim());
            if (targetPostId != null) body.put("targetPostId", targetPostId);
            if (feedbackPostId != null) body.put("feedbackPostId", feedbackPostId);
            if (draftPostId != null) body.put("draftPostId", draftPostId);
            body.put("unreadOnly", unreadOnly);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/chat"))
                    // Agent 可能需要一次模型调用加一次社区工具调用，15 秒容易在正常响应前超时。
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("X-AI-Service-Token", serviceToken)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("AI service returned status={}, body={}", response.statusCode(), response.body());
                throw new AiServiceException("AI 服务暂时不可用");
            }
            JSONObject result = JSON.parseObject(response.body());
            String reply = result.getString("reply");
            String summary = result.getString("summary");
            if (reply == null || reply.isBlank()) reply = "暂时无法完成检索";
            List<Integer> postIds = new ArrayList<>();
            JSONArray ids = result.getJSONArray("postIds");
            if (ids != null) ids.forEach(node -> { if (node instanceof Number) postIds.add(((Number) node).intValue()); });
            return new AiChatVO(currentConversationId, reply, summary, buildCards(postIds));
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI service request failed, url={}", serviceUrl, e);
            throw new AiServiceException("AI 服务暂时不可用", e);
        }
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return UUID.randomUUID().toString();
        try {
            return UUID.fromString(conversationId).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("会话标识无效");
        }
    }

    private List<AiPostCardVO> buildCards(List<Integer> postIds) {
        if (postIds.isEmpty()) return List.of();
        Map<Integer, Post> postsById = new LinkedHashMap<>();
        postService.listByIds(postIds).stream()
                .filter(post -> !Boolean.TRUE.equals(post.getDeleted()))
                .forEach(post -> postsById.put(post.getId(), post));
        return postIds.stream().distinct()
                .map(postsById::get)
                .filter(java.util.Objects::nonNull)
                .map(post -> new AiPostCardVO(
                        post.getId(),
                        post.getTitle(),
                        snippet(post.getContent()),
                        "/post/" + post.getId()))
                .toList();
    }

    private String snippet(String content) {
        if (content == null) return "";
        String normalized = content.strip();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160) + "...";
    }

    public static class AiServiceException extends RuntimeException {
        public AiServiceException(String message) { super(message); }
        public AiServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
