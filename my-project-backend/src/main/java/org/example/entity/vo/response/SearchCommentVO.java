package org.example.entity.vo.response;
import java.util.Date;
public record SearchCommentVO(Integer commentId, Integer postId, String postTitle, String authorNickname, String content, Date createTime) {}
