package org.example.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class PostCreateVO {
    @NotBlank(message = "标题不能为空")
    @Length(max = 100, message = "标题不能超过100字")
    String title;

    @NotBlank(message = "正文不能为空")
    @Length(max = 10000, message = "正文不能超过10000字")
    String content;

    @NotBlank(message = "资源链接不能为空")
    @Pattern(regexp = "^https?://\\S+$", message = "资源链接必须以 http(s):// 开头")
    @Length(max = 2048, message = "资源链接不能超过2048个字符")
    String resourceUrl;
}
