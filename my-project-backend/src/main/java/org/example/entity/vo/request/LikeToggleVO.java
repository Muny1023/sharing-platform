package org.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeToggleVO {
    /**
     * 点赞目标类型：post 帖子 / comment 评论。
     */
    @NotNull(message = "目标类型不能为空")
    String targetType;

    /**
     * 目标ID：帖子ID或评论ID。
     */
    @NotNull(message = "目标ID不能为空")
    Integer targetId;
}
