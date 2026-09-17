package org.example.controller;

import org.example.entity.vo.request.AiChatRequestVO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiChatRequestVOTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void missingUnreadOnlySupportsExistingAiRequests() throws Exception {
        AiChatRequestVO request = mapper.readValue("{\"message\":\"你好\"}", AiChatRequestVO.class);

        assertEquals("你好", request.message());
        assertNull(request.unreadOnly());
    }
}
