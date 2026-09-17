package org.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiPostContentRequestVO(
        @NotNull @Positive Integer postId,
        @NotNull @Positive Integer userId) {
}
