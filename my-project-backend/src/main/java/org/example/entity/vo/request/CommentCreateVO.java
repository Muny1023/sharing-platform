package org.example.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CommentCreateVO {
    @NotNull(message = "帖子ID不能为空")
    Integer postId;

    /**
     * 父评论ID，null 表示顶级评论。只允许两级：顶级评论的 parentId 为 null，回复指向顶级评论。
     */
    Integer parentId;

    @NotBlank(message = "评论内容不能为空")
    @Length(max = 1000, message = "评论不能超过1000字")
    String content;
}
