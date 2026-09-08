package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data @TableName("db_post_tombstone") @AllArgsConstructor @NoArgsConstructor
public class PostTombstone {
    @TableId Integer postId;
    Date deletedAt;
    Date expireAt;
}
