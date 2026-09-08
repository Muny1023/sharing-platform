package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.dto.Like;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
