package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.dto.Post;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    @Select("SELECT p.* FROM db_post p WHERE p.deleted=0 ORDER BY p.like_count DESC, p.comment_count DESC, p.favorite_count DESC, p.create_time DESC")
    Page<Post> selectHotPage(Page<Post> page);

    @Select("SELECT p.* FROM db_post p WHERE p.deleted=0 AND p.author_id=#{authorId} ORDER BY p.like_count DESC, p.comment_count DESC, p.favorite_count DESC, p.create_time DESC")
    Page<Post> selectHotPageByAuthor(Page<Post> page, Integer authorId);
}
