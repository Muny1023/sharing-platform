package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_post")
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer authorId;
    String title;
    String content;
    String resourceUrl;
    Integer likeCount;
    Integer commentCount;
    Date createTime;
    Date updateTime;
    Boolean deleted;
    Integer favoriteCount;

    public Post(Integer id, Integer authorId, String title, String content, String resourceUrl,
                Integer likeCount, Integer commentCount, Date createTime, Date updateTime) {
        this(id, authorId, title, content, resourceUrl, likeCount, commentCount, createTime, updateTime, false, 0);
    }
}
