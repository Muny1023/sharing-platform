USE test;

CREATE TABLE IF NOT EXISTS db_notification (
    id INT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    recipient_id INT NOT NULL COMMENT '接收通知的账号ID',
    actor_id INT NOT NULL COMMENT '触发通知的账号ID',
    type VARCHAR(32) NOT NULL COMMENT '通知类型：POST_LIKE、POST_COMMENT、COMMENT_REPLY',
    post_id INT NULL COMMENT '相关帖子ID',
    comment_id INT NULL COMMENT '相关评论ID',
    content VARCHAR(500) NOT NULL COMMENT '通知内容快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    event_id VARCHAR(64) NOT NULL COMMENT '业务事件幂等ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_event_id (event_id),
    INDEX idx_notification_recipient_time (recipient_id, created_at),
    INDEX idx_notification_recipient_read (recipient_id, is_read),
    INDEX idx_notification_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '互动通知表';
