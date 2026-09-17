package org.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;

public record AiPostFeedbackRequestVO(@NotNull Integer postId, @NotNull Integer userId, boolean unreadOnly) {}
