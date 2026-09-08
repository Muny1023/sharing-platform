package org.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Account;
import org.example.entity.dto.Favorite;
import org.example.entity.dto.Post;
import org.example.entity.vo.response.FavoriteItemVO;
import org.example.entity.vo.response.PageVO;
import org.example.mapper.FavoriteMapper;
import org.example.service.AccountService;
import org.example.service.FavoriteService;
import org.example.service.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {
    @Resource FavoriteMapper favoriteMapper;
    @Resource PostService postService;
    @Resource AccountService accountService;
    @Override @Transactional public ToggleResult toggle(Integer userId, Integer postId) {
        Favorite old = getOne(Wrappers.<Favorite>query().eq("user_id", userId).eq("post_id", postId));
        Post post = postService.getById(postId);
        if (post == null) {
            if (old != null && removeById(old.getId())) return new ToggleResult(false, null);
            return new ToggleResult(false, "帖子不存在");
        }
        if (old != null) {
            if (!removeById(old.getId())) return new ToggleResult(true, "内部错误，请联系管理员");
            postService.update().eq("id", postId).setSql("favorite_count = GREATEST(favorite_count - 1, 0)").update();
            return new ToggleResult(false, null);
        }
        if (!save(new Favorite(null, userId, postId, new Date(), false))) return new ToggleResult(false, "内部错误，请联系管理员");
        postService.update().eq("id", postId).setSql("favorite_count = favorite_count + 1").update();
        return new ToggleResult(true, null);
    }
    @Override public boolean isFavorited(Integer userId, Integer postId) { return exists(Wrappers.<Favorite>query().eq("user_id", userId).eq("post_id", postId)); }
    @Override public PageVO<FavoriteItemVO> listMine(Integer userId, int page, int size, String sort) {
        Page<Favorite> result = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50));
        var query = Wrappers.<Favorite>query().eq("user_id", userId);
        if ("hot".equals(sort)) favoriteMapper.selectByHot(result, userId);
        else { if ("oldest".equals(sort)) query.orderByAsc("create_time"); else query.orderByDesc("create_time"); page(result, query); }
        List<Integer> ids = result.getRecords().stream().map(Favorite::getPostId).toList();
        Map<Integer, Post> posts = ids.isEmpty() ? Map.of() : postService.listByIds(ids).stream().collect(Collectors.toMap(Post::getId, p -> p));
        Map<Integer, String> names = posts.isEmpty() ? Map.of() : accountService.listByIds(posts.values().stream().map(Post::getAuthorId).distinct().toList()).stream().collect(Collectors.toMap(Account::getId, Account::getNickname, (a,b)->a));
        List<FavoriteItemVO> items = result.getRecords().stream().map(f -> {
            Post p = posts.get(f.getPostId()); boolean deleted = Boolean.TRUE.equals(f.getPostDeleted()) || p == null || Boolean.TRUE.equals(p.getDeleted());
            return new FavoriteItemVO(f.getId(), f.getPostId(), p == null ? "" : names.getOrDefault(p.getAuthorId(), "未知用户"), deleted ? "" : p.getTitle(), deleted ? "" : p.getContent(), deleted ? "" : p.getResourceUrl(), p == null ? 0 : p.getLikeCount(), p == null ? 0 : p.getCommentCount(), p == null ? null : p.getCreateTime(), f.getCreateTime(), deleted);
        }).toList();
        return new PageVO<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }
}
