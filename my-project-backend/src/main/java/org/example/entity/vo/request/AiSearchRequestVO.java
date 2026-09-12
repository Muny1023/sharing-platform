package org.example.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AiSearchRequestVO(
        String keyword,
        @Min(1) int page,
        @Min(1) @Max(10) int size) {
}
