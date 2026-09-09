package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("db_notification")
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer recipientId;
    private Integer actorId;
    private String type;
    private Integer postId;
    private Integer commentId;
    private String content;
    private Date createdAt;
    private Boolean isRead;
    private String eventId;
}
