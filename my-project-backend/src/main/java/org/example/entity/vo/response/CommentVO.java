package org.example.entity.vo.response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public record CommentVO(
        Integer id,
        Integer postId,
        Integer authorId,
        String authorNickname,
        Integer parentId,
        String content,
        Integer likeCount,
        Date createTime,
        List<CommentVO> replies) {

    public CommentVO {
        replies = replies == null ? new ArrayList<>() : replies;
    }
}
