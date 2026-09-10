package org.example.entity.vo.response;

import java.util.Date;

public record PostDetailVO(
        Integer id,
        Integer authorId,
        String authorNickname,
        String title,
        String content,
        String resourceUrl,
        Integer likeCount,
        Integer commentCount,
        Date createTime,
        Date updateTime,
        boolean deleted,
        Integer favoriteCount) {
    public PostDetailVO(Integer id, Integer authorId, String authorNickname, String title, String content, String resourceUrl, Integer likeCount, Integer commentCount, Date createTime, Date updateTime) {
        this(id, authorId, authorNickname, title, content, resourceUrl, likeCount, commentCount, createTime, updateTime, false, 0);
    }
}
