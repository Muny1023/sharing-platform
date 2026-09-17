package org.example.entity.vo.response;

import java.util.Date;

public record AiCommentItemVO(Integer id, Integer parentId, String content, Date createTime) {}
