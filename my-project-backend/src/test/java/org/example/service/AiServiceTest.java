package org.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiServiceTest {

    @Test
    void invalidConversationIdIsRejectedBeforeCallingPython() {
        AiService service = new AiService();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.chat(1, "legacy-session", null, null, null, false, "你好"));

        assertEquals("会话标识无效", exception.getMessage());
    }
}
