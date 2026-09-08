package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.entity.dto.PostTombstone;
import java.util.List;

@Mapper public interface PostTombstoneMapper extends BaseMapper<PostTombstone> {
    @Select("SELECT * FROM db_post_tombstone WHERE expire_at <= NOW() LIMIT 500")
    List<PostTombstone> selectExpired();
    @Delete("DELETE FROM db_post_tombstone WHERE post_id=#{postId}")
    int deleteByPostId(Integer postId);
}
