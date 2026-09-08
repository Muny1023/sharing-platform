USE test;

ALTER TABLE db_post ADD COLUMN favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数量';
UPDATE db_post p SET favorite_count = (SELECT COUNT(*) FROM db_favorite f WHERE f.post_id = p.id);
