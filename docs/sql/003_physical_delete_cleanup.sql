USE test;

-- 清理上一版软删除留下的帖子及其评论、点赞和收藏级联数据。
DELETE FROM db_post WHERE deleted = 1;
