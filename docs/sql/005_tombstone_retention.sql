USE test;

-- 允许帖子物理删除后保留失效收藏关系。
ALTER TABLE db_favorite DROP FOREIGN KEY fk_favorite_post;
ALTER TABLE db_favorite ADD COLUMN post_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '帖子是否已删除';

CREATE TABLE IF NOT EXISTS db_post_tombstone (
    post_id INT NOT NULL COMMENT '已删除帖子ID',
    deleted_at DATETIME NOT NULL COMMENT '删除时间',
    expire_at DATETIME NOT NULL COMMENT '墓碑到期时间',
    PRIMARY KEY (post_id),
    INDEX idx_tombstone_expire_at (expire_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子墓碑记录表';
