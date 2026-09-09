package org.example.entity.vo.response;

import java.util.Date;

public record NotificationVO(
        Integer id,
        Integer actorId,
        String actorNickname,
        String type,
        Integer postId,
        Integer commentId,
        String content,
        Date createdAt,
        boolean read,
        boolean postDeleted,
        boolean commentDeleted) {
}
