package org.example.entity.vo.response;

import java.util.List;

public record AiChatVO(
        String conversationId,
        String reply,
        List<AiPostCardVO> posts) {
}
