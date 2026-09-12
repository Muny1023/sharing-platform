package org.example.entity.vo.response;

public record AiPostCardVO(
        Integer postId,
        String title,
        String snippet,
        String path) {
}
