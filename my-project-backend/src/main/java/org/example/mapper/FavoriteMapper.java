package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.dto.Favorite;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("SELECT f.* FROM db_favorite f LEFT JOIN db_post p ON p.id=f.post_id WHERE f.user_id=#{userId} ORDER BY p.like_count DESC, f.create_time DESC")
    IPage<Favorite> selectByHot(Page<Favorite> page, Integer userId);

    @Update("UPDATE db_favorite SET post_deleted=1 WHERE post_id=#{postId}")
    int markPostDeleted(Integer postId);

    @Delete("DELETE FROM db_favorite WHERE post_deleted=1 AND post_id IN (SELECT post_id FROM db_post_tombstone WHERE expire_at <= NOW())")
    int deleteExpiredInvalidFavorites();
}
