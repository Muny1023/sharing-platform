package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_comment")
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer postId;
    Integer authorId;
    Integer parentId;
    String content;
    Integer likeCount;
    Date createTime;
    Date updateTime;
}
