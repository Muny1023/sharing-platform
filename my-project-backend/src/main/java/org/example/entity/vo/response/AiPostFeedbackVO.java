package org.example.entity.vo.response;

import java.util.Date;
import java.util.List;

public record AiPostFeedbackVO(Integer postId, String content, Date updateTime, List<AiCommentItemVO> comments, boolean partial) {}
