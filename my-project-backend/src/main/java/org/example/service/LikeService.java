package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Like;
import org.example.entity.vo.request.LikeToggleVO;

import java.util.List;
import java.util.Set;

public interface LikeService extends IService<Like> {
    /**
     * 幂等切换点赞：未赞则赞，已赞则取消。
     * @return message 为 null 表示成功，此时 liked = 切换后是否处于已赞状态，likeCount = 切换后的点赞数。
     */
    LikeToggleResult toggleLike(Integer userId, LikeToggleVO vo);

    /**
     * 批量查询当前用户在一批目标（帖子或评论）里已点赞的目标 id 集合，供前端渲染初始点赞状态。
     */
    Set<Integer> findLikedTargetIds(Integer userId, String targetType, List<Integer> targetIds);

    record LikeToggleResult(String message, boolean liked, int likeCount) {
        public static LikeToggleResult success(boolean liked, int likeCount) {
            return new LikeToggleResult(null, liked, likeCount);
        }
        public static LikeToggleResult failure(String message) {
            return new LikeToggleResult(message, false, 0);
        }
    }
}
