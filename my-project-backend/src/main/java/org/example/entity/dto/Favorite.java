package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data @TableName("db_favorite") @AllArgsConstructor @NoArgsConstructor
public class Favorite {
    @TableId(type = IdType.AUTO) Integer id;
    Integer userId;
    Integer postId;
    Date createTime;
    Boolean postDeleted;

    public Favorite(Integer id, Integer userId, Integer postId, Date createTime) {
        this(id, userId, postId, createTime, false);
    }
}
