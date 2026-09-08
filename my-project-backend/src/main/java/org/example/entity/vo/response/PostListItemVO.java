package org.example.entity.vo.response;

import java.util.Date;

public record PostListItemVO(
        Integer id,
        Integer authorId,
        String authorNickname,
        String title,
        String content,
        String resourceUrl,
        Integer likeCount,
        Integer commentCount,
        Date createTime,
        boolean deleted,
        Integer favoriteCount) {
    public PostListItemVO(Integer id, Integer authorId, String authorNickname, String title, String content, String resourceUrl, Integer likeCount, Integer commentCount, Date createTime) {
        this(id, authorId, authorNickname, title, content, resourceUrl, likeCount, commentCount, createTime, false, 0);
    }
}
