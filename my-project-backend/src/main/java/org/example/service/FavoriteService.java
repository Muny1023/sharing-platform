package org.example.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Favorite;
import org.example.entity.vo.response.FavoriteItemVO;
import org.example.entity.vo.response.PageVO;
public interface FavoriteService extends IService<Favorite> {
    ToggleResult toggle(Integer userId, Integer postId);
    boolean isFavorited(Integer userId, Integer postId);
    PageVO<FavoriteItemVO> listMine(Integer userId, int page, int size, String sort);
    record ToggleResult(boolean favorited, String message) {}
}
