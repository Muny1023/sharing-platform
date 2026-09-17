USE test;

-- Apply this migration to databases created before the community soft-delete field was introduced.
ALTER TABLE db_post ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记';
CREATE INDEX idx_post_deleted ON db_post (deleted);

CREATE TABLE IF NOT EXISTS db_favorite (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_post (user_id, post_id),
    INDEX idx_favorite_user_time (user_id, create_time),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES db_account (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_favorite_post FOREIGN KEY (post_id) REFERENCES db_post (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
