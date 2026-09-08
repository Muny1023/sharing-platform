-- Resource-sharing community schema archive.
-- These statements were executed manually in Navicat on database `test`.

USE test;

ALTER TABLE db_account
    ADD COLUMN nickname VARCHAR(50) NOT NULL DEFAULT '' COMMENT '用户昵称',
    ADD COLUMN avatar VARCHAR(500) NULL COMMENT '头像外链地址';

UPDATE db_account SET nickname = username WHERE nickname = '';

CREATE TABLE IF NOT EXISTS db_post (
    id INT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    author_id INT NOT NULL COMMENT '作者账号ID',
    title VARCHAR(100) NOT NULL COMMENT '帖子标题',
    content TEXT NOT NULL COMMENT '帖子正文',
    resource_url VARCHAR(2048) NOT NULL COMMENT '资源外链地址',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数量',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数量',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记',
    PRIMARY KEY (id),
    INDEX idx_post_author_id (author_id),
    INDEX idx_post_create_time (create_time),
    INDEX idx_post_like_count (like_count),
    INDEX idx_post_deleted (deleted),
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES db_account (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '资源分享帖子表';

CREATE TABLE IF NOT EXISTS db_comment (
    id INT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    post_id INT NOT NULL COMMENT '所属帖子ID',
    author_id INT NOT NULL COMMENT '评论作者账号ID',
    parent_id INT NULL COMMENT '父评论ID，NULL表示顶级评论',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    INDEX idx_comment_post_id (post_id),
    INDEX idx_comment_author_id (author_id),
    INDEX idx_comment_parent_id (parent_id),
    INDEX idx_comment_create_time (create_time),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES db_post (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES db_account (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES db_comment (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子评论表';

CREATE TABLE IF NOT EXISTS db_like (
    id INT NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
    user_id INT NOT NULL COMMENT '点赞用户ID',
    post_id INT NULL COMMENT '被点赞的帖子ID',
    comment_id INT NULL COMMENT '被点赞的评论ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (id),
    INDEX idx_like_user_id (user_id),
    INDEX idx_like_post_id (post_id),
    INDEX idx_like_comment_id (comment_id),
    UNIQUE KEY uk_like_user_post (user_id, post_id),
    UNIQUE KEY uk_like_user_comment (user_id, comment_id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES db_account (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_like_post FOREIGN KEY (post_id) REFERENCES db_post (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_like_comment FOREIGN KEY (comment_id) REFERENCES db_comment (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子和评论点赞表';

CREATE TABLE IF NOT EXISTS db_favorite (
    id INT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
    user_id INT NOT NULL COMMENT '收藏账号ID',
    post_id INT NOT NULL COMMENT '收藏帖子ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    post_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '帖子是否已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_post (user_id, post_id),
    INDEX idx_favorite_user_time (user_id, create_time),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES db_account (id) ON DELETE CASCADE ON UPDATE CASCADE,
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子收藏表';

CREATE TABLE IF NOT EXISTS db_post_tombstone (
    post_id INT NOT NULL COMMENT '已删除帖子ID',
    deleted_at DATETIME NOT NULL COMMENT '删除时间',
    expire_at DATETIME NOT NULL COMMENT '墓碑到期时间',
    PRIMARY KEY (post_id),
    INDEX idx_tombstone_expire_at (expire_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '帖子墓碑记录表';
