package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_like")
@AllArgsConstructor
@NoArgsConstructor
public class Like {
    @TableId(type = IdType.AUTO)
    Integer id;
    Integer userId;
    Integer postId;
    Integer commentId;
    Date createTime;
}
