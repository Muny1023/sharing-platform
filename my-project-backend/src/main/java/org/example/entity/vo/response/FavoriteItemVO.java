package org.example.entity.vo.response;
import java.util.Date;
public record FavoriteItemVO(Integer favoriteId, Integer postId, String authorNickname, String title, String content, String resourceUrl, Integer likeCount, Integer commentCount, Date postCreateTime, Date favoriteCreateTime, boolean deleted) {}
