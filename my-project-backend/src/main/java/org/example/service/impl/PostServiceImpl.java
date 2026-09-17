package org.example.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Account;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.PostCreateVO;
import org.example.entity.vo.response.PageVO;
import org.example.entity.vo.response.PostDetailVO;
import org.example.entity.vo.response.PostListItemVO;
import org.example.entity.vo.response.SearchCommentVO;
import org.example.entity.vo.request.PostUpdateVO;
import org.example.entity.dto.Comment;
import org.example.mapper.CommentMapper;
import org.example.mapper.PostMapper;
import org.example.mapper.FavoriteMapper;
import org.example.mapper.PostTombstoneMapper;
import org.example.entity.dto.PostTombstone;
import org.example.service.AccountService;
import org.example.service.PostService;
import org.example.utils.Const;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
    private static final String DETAIL_CACHE_PREFIX = "post:detail:";
    private static final long DETAIL_CACHE_MINUTES = 10;

    private static final int POST_CREATE_INTERVAL_SECONDS = 30;
    private static final long DAILY_POST_LIMIT = 50;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    AccountService accountService;
    @Resource CommentMapper commentMapper;
    @Resource PostMapper postMapper;
    @Resource FavoriteMapper favoriteMapper;
    @Resource PostTombstoneMapper tombstoneMapper;

    @Override
    public String createPost(Integer authorId, PostCreateVO vo) {
        String intervalMessage = this.checkPostInterval(authorId);
        if (intervalMessage != null) return intervalMessage;
        String dailyLimitMessage = this.checkDailyPostLimit(authorId);
        if (dailyLimitMessage != null) return dailyLimitMessage;

        Post post = new Post(null, authorId, vo.getTitle(), vo.getContent(), vo.getResourceUrl(), 0, 0, new Date(), new Date(), false, 0);
        return this.save(post) ? null : "内部错误，请联系管理员";
    }

    private String checkPostInterval(Integer authorId) {
        Boolean accepted = stringRedisTemplate.opsForValue().setIfAbsent(
                Const.POST_CREATE_INTERVAL + authorId, "", POST_CREATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(accepted) ? null : "30秒内不能重复发帖，请稍后再试";
    }

    private String checkDailyPostLimit(Integer authorId) {
        String key = Const.POST_CREATE_DAILY + authorId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, 1, TimeUnit.DAYS);
        }
        return count != null && count <= DAILY_POST_LIMIT ? null : "今日发帖已达上限（每天最多发布50帖）";
    }

    @Override
    public PageVO<PostListItemVO> listPosts(int page, int size, String sort) {
        Page<Post> pager = new Page<>(page, size);
        var query = Wrappers.<Post>query().eq("deleted", false);
        if ("hot".equals(sort)) {
            postMapper.selectHotPage(pager);
        } else if ("oldest".equals(sort)) {
            query.orderByAsc("create_time");
        } else {
            query.orderByDesc("create_time");
        }
        Page<Post> result = "hot".equals(sort) ? pager : this.page(pager, query);
        Map<Integer, String> nicknameMap = this.loadNicknames(result.getRecords());
        List<PostListItemVO> items = result.getRecords().stream()
                .map(post -> new PostListItemVO(
                        post.getId(), post.getAuthorId(),
                        nicknameMap.getOrDefault(post.getAuthorId(), "未知用户"),
                        post.getTitle(), post.getContent(), post.getResourceUrl(),
                        post.getLikeCount(), post.getCommentCount(), post.getCreateTime(), Boolean.TRUE.equals(post.getDeleted()), post.getFavoriteCount()))
                .toList();
        return new PageVO<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }

    @Override
    public PostDetailVO getPostDetail(Integer id) {
        String cacheKey = DETAIL_CACHE_PREFIX + id;
        String cached = cacheGet(cacheKey);
        if (cached != null) {
            try {
                log.debug("post detail cache hit, id={}", id);
                return JSON.parseObject(cached, PostDetailVO.class);
            } catch (RuntimeException e) {
                log.warn("invalid post detail cache, evicting, id={}", id);
                cacheDelete(cacheKey);
            }
        }
        log.debug("post detail cache miss, id={}", id);
        Post post = this.getById(id);
        if (post == null) return null;
        String nickname = accountService.findAccountById(post.getAuthorId())
                .map(Account::getNickname)
                .orElse("未知用户");
        PostDetailVO detail = new PostDetailVO(
                post.getId(), post.getAuthorId(), nickname,
                post.getTitle(), post.getContent(), post.getResourceUrl(),
                post.getLikeCount(), post.getCommentCount(),
                post.getCreateTime(), post.getUpdateTime(), Boolean.TRUE.equals(post.getDeleted()), post.getFavoriteCount());
        cacheSet(cacheKey, JSON.toJSONString(detail));
        return detail;
    }

    @Override public String updatePost(Integer authorId, Integer postId, PostUpdateVO vo) {
        Post post = getById(postId);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) return "帖子不存在或已被删除";
        if (!authorId.equals(post.getAuthorId())) return "只能编辑自己的帖子";
        var updater = update().eq("id", postId).eq("author_id", authorId);
        if (vo.getExpectedUpdateTime() != null) updater.eq("update_time", new Date(vo.getExpectedUpdateTime()));
        boolean updated = updater.set("title", vo.getTitle()).set("content", vo.getContent()).set("resource_url", vo.getResourceUrl()).update();
        if (updated) cacheDelete(DETAIL_CACHE_PREFIX + postId);
        return updated ? null : (vo.getExpectedUpdateTime() != null ? "帖子内容已被修改，请重新生成草稿" : "内部错误，请联系管理员");
    }
    @Override @Transactional public String deletePost(Integer authorId, Integer postId) {
        Post post = getById(postId);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) return "帖子不存在或已被删除";
        if (!authorId.equals(post.getAuthorId())) return "只能删除自己的帖子";
        Date now = new Date();
        tombstoneMapper.insert(new PostTombstone(postId, now, new Date(now.getTime() + TimeUnit.DAYS.toMillis(1))));
        favoriteMapper.markPostDeleted(postId);
        // 帖子本体物理删除，评论和点赞由数据库外键级联清理；收藏关系保留为失效收藏。
        boolean removed = removeById(postId);
        if (removed) cacheDelete(DETAIL_CACHE_PREFIX + postId);
        return removed ? null : "内部错误，请联系管理员";
    }
    @Override public PageVO<PostListItemVO> listMyPosts(Integer authorId, int page, int size, String sort) {
        Page<Post> result = new Page<>(page, size);
        var q = Wrappers.<Post>query().eq("author_id", authorId).eq("deleted", false);
        if ("oldest".equals(sort)) q.orderByAsc("create_time"); else if (!"hot".equals(sort)) q.orderByDesc("create_time");
        if ("hot".equals(sort)) postMapper.selectHotPageByAuthor(result, authorId); else page(result, q);
        return toPageVO(result);
    }
    @Override public PageVO<PostListItemVO> searchPosts(String keyword, int page, int size) {
        Page<Post> result = new Page<>(page, size);
        var query = Wrappers.<Post>query().eq("deleted", false);
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim().replaceAll("([A-Za-z0-9]+)", " $1 ");
            Arrays.stream(normalized.split("\\s+"))
                    .filter(token -> !token.isBlank())
                    .forEach(token -> query.and(w -> w.like("title", token).or().like("content", token)));
        }
        page(result, query.orderByDesc("create_time"));
        return toPageVO(result);
    }
    @Override public PageVO<SearchCommentVO> searchComments(String keyword, int page, int size) {
        Page<Comment> result = new Page<>(page, size);
        commentMapper.selectPage(result, Wrappers.<Comment>query().like("content", keyword).orderByDesc("create_time"));
        Map<Integer, Post> posts = result.getRecords().isEmpty() ? Map.of() : listByIds(result.getRecords().stream().map(Comment::getPostId).distinct().toList()).stream().collect(Collectors.toMap(Post::getId,p->p));
        Map<Integer,String> names = accountService.listByIds(result.getRecords().stream().map(Comment::getAuthorId).distinct().toList()).stream().collect(Collectors.toMap(Account::getId,Account::getNickname,(a,b)->a));
        List<SearchCommentVO> items = result.getRecords().stream().filter(c -> posts.containsKey(c.getPostId()) && !Boolean.TRUE.equals(posts.get(c.getPostId()).getDeleted())).map(c -> new SearchCommentVO(c.getId(),c.getPostId(),posts.get(c.getPostId()).getTitle(),names.getOrDefault(c.getAuthorId(),"未知用户"),c.getContent(),c.getCreateTime())).toList();
        return new PageVO<>(result.getCurrent(), result.getSize(), result.getTotal(), items);
    }
    private PageVO<PostListItemVO> toPageVO(Page<Post> result) {
        Map<Integer,String> names=loadNicknames(result.getRecords());
        return new PageVO<>(result.getCurrent(),result.getSize(),result.getTotal(),result.getRecords().stream().map(p->new PostListItemVO(p.getId(),p.getAuthorId(),names.getOrDefault(p.getAuthorId(),"未知用户"),p.getTitle(),p.getContent(),p.getResourceUrl(),p.getLikeCount(),p.getCommentCount(),p.getCreateTime(),Boolean.TRUE.equals(p.getDeleted()),p.getFavoriteCount())).toList());
    }

    /**
     * 批量取作者昵称，避免列表页逐条查库。
     */
    private Map<Integer, String> loadNicknames(List<Post> posts) {
        List<Integer> authorIds = posts.stream().map(Post::getAuthorId).distinct().toList();
        if (authorIds.isEmpty()) return Map.of();
        return accountService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getNickname, (a, b) -> a));
    }

    private String cacheGet(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("redis unavailable, bypassing cache, key={}", key);
            return null;
        }
    }

    private void cacheSet(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, DETAIL_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            log.warn("redis unavailable, skip cache write, key={}", key);
        }
    }

    private void cacheDelete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("redis unavailable, skip cache eviction, key={}", key);
        }
    }
}
